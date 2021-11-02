package com.github._1c_syntax.reader.designer.wrapper;

import com.github._1c_syntax.bsl.mdo.MDObject;
import com.github._1c_syntax.bsl.mdo.Module;
import com.github._1c_syntax.bsl.mdo.children.ObjectModule;
import com.github._1c_syntax.bsl.mdo.support.ApplicationUsePurpose;
import com.github._1c_syntax.bsl.mdo.support.MdoReference;
import com.github._1c_syntax.bsl.support.SupportVariant;
import com.github._1c_syntax.bsl.types.ConfigurationSource;
import com.github._1c_syntax.bsl.types.MDOType;
import com.github._1c_syntax.bsl.types.ModuleType;
import com.github._1c_syntax.mdclasses.utils.MDOPathUtils;
import com.github._1c_syntax.mdclasses.utils.MDOUtils;
import com.github._1c_syntax.mdclasses.utils.TransformationUtils;
import com.github._1c_syntax.reader.designer.DesignerXStreamFactory;
import com.github._1c_syntax.reader.designer.converter.DesignerConverterCommon;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import lombok.Data;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
public class DesignerProperties {

  private static final String PROPERTIES_NODE_NAME = "Properties";
  private static final String CHILD_OBJECTS_NODE_NAME = "ChildObjects";

  private Map<String, Object> properties;
  private Map<String, Object> unknownProperties;
  private List<Object> children;

  private String realClassName;
  private Class<?> realClass;
  private Object builder;

  private MDOType mdoType;
  private Path currentPath;

  private String name;
  private SupportVariant supportVariant;

  private MdoReference mdoReference;

  public DesignerProperties(@NonNull HierarchicalStreamReader reader, @NonNull UnmarshallingContext context) {

    realClassName = reader.getNodeName();
    realClass = DesignerXStreamFactory.getRealClass(realClassName);
    builder = TransformationUtils.builder(realClass);
    requireNonNull(builder);

    mdoType = getMdoType(reader);
    currentPath = DesignerXStreamFactory.getCurrentPath(reader);

    properties = new HashMap<>();
    properties.put("uuid", reader.getAttribute("uuid"));

    unknownProperties = new HashMap<>();

    children = new ArrayList<>();

    // линейно читаем файл
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      var nodeName = reader.getNodeName();
      switch (nodeName) {
        case PROPERTIES_NODE_NAME:
          readProperties(reader, context);

          name = (String) properties.get("Name");
          mdoReference = MdoReference.create(mdoType, name);
          supportVariant = SupportVariant.NONE;// todo Заглушка

          break;
        case CHILD_OBJECTS_NODE_NAME:
          readChildren(reader, context);
          if (MDOType.valuesWithoutChildren().contains(mdoType)) {
            buildWithChildren();
          }
          break;
        default:
          // no-op
          break;
      }

      reader.moveUp();
    }

    readModules();
  }

  public void updateOwner(MdoReference owner) {
    setMdoReference(MdoReference.create(owner, mdoType, name));
    properties.put("owner", owner);

    // если есть дочерние, им тоже стоит обновиться
    children.stream().filter(DesignerProperties.class::isInstance)
      .forEach(child -> ((DesignerProperties) child).updateOwner(mdoReference));
  }

  public Object buildWithChildren() {
    if (!children.isEmpty()) {
      var correctChildren = children.stream()
        .filter(MDObject.class::isInstance)
        .collect(Collectors.toList());

      if (correctChildren.size() != children.size()) {
        correctChildren.addAll(children.stream()
          .filter(DesignerProperties.class::isInstance)
          .map(DesignerProperties.class::cast)
          .map(DesignerProperties::buildWithChildren)
          .collect(Collectors.toList()));

        setChildren(correctChildren);
      }
    }

    if (!MDOType.valuesWithoutChildren().contains(mdoType)) {
      DesignerConverterCommon.computeBuilder(builder, this);
      return TransformationUtils.build(builder);
    } else {
      // эти билдятся отдельно
      return this;
    }
  }

  private void readProperties(HierarchicalStreamReader reader, UnmarshallingContext context) {
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      var propertyName = reader.getNodeName();
      var value = readValue(reader, context, propertyName);
      if (value != null) {
        properties.put(propertyName, value);
      }
      reader.moveUp();
    }
  }

  private void readChildren(HierarchicalStreamReader reader, UnmarshallingContext context) {

    while (reader.hasMoreChildren()) {
      reader.moveDown();
      var nodeName = reader.getNodeName();
      try {
        Class<?> childRealClass = DesignerXStreamFactory.getRealClass(nodeName);
        if (childRealClass == null) {
          throw new IllegalStateException("Unexpected type: " + nodeName);
        }

        var child = context.convertAnother(reader, childRealClass);
        if (child instanceof DesignerProperties) {
          // нужно обновить ссылки на родителя
          ((DesignerProperties) child).updateOwner(mdoReference);
        }

        children.add(child);

      } catch (Exception e) {
//        System.out.println("Cannot find class for " + nodeName + "\n" + e);
      }

      reader.moveUp();
    }
  }

  private void readModules() {
    var mdoFolderPath = MDOPathUtils.getMDOTypeFolderByMDOPath(currentPath, mdoType);
    if (mdoFolderPath.isEmpty()) {
      return;
    }

    var folder = mdoFolderPath.get();
    var moduleTypes = MDOUtils.getModuleTypesForMdoTypes().getOrDefault(mdoType, Collections.emptySet());
    if (moduleTypes.isEmpty()) {
      return;
    }

    List<Module> modules = new ArrayList<>();
    moduleTypes.forEach((ModuleType moduleType) ->
      MDOPathUtils.getModulePath(ConfigurationSource.DESIGNER, folder, name, moduleType)
        .ifPresent((Path modulePath) -> {
          if (modulePath.toFile().exists()) {
            modules.add(ObjectModule.builder()
              .moduleType(moduleType)
              .uri(modulePath.toUri())
              .owner(mdoReference)
              .supportVariant(supportVariant)
              .build());
          }
        }));
    properties.put("modules", modules);
  }

  @Nullable
  private Object readValue(HierarchicalStreamReader reader, UnmarshallingContext context, String propertyName) {
    var methodType = TransformationUtils.fieldType(builder, propertyName);
    if (methodType == null) {
      unknownProperties.put(propertyName, reader.getValue());
      return null;
    }

    Object value = null;

    try {
      if (methodType instanceof ParameterizedType
        && ApplicationUsePurpose.class
        .isAssignableFrom((Class<?>) ((ParameterizedType) methodType)
          .getActualTypeArguments()[0])) {

        value = context.convertAnother(reader, (Class<?>) ((ParameterizedType) methodType)
          .getActualTypeArguments()[0]);

      } else if (methodType instanceof ParameterizedType) {
        List<Object> values = new ArrayList<>();
        var clazz = (Class<?>) ((ParameterizedType) methodType)
          .getActualTypeArguments()[0];
        while (reader.hasMoreChildren()) {
          reader.moveDown();
          values.add(context.convertAnother(reader, clazz));
          reader.moveUp();
        }
        value = values;
      } else {
        value = context.convertAnother(reader, (Class<?>) methodType);
      }
    } catch (Exception e) {
//        System.out.println("Cannot convert " + propertyName + " to type " + methodType + "\n" + e);
//          undefinedProperties.put(propertyName, reader.getValue());
    }
//    } else {
//      System.out.println("Undefined node " + propertyName);
//        undefinedProperties.put(propertyName, reader.getValue());

    return value;
  }

  private MDOType getMdoType(HierarchicalStreamReader reader) {
    var type = MDOType.UNKNOWN;
    var computedMdoType = MDOType.fromValue(reader.getNodeName());
    if (computedMdoType.isPresent()) {
      type = computedMdoType.get();
    }
    return type;
  }

}