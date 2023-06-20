/*
 * This file is a part of MDClasses.
 *
 * Copyright © 2019 - 2021
 * Tymko Oleg <olegtymko@yandex.ru>, Maximov Valery <maximovvalery@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * MDClasses is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * MDClasses is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with MDClasses.
 */
package com.github._1c_syntax.mdclasses.mdo;

import com.github._1c_syntax.mdclasses.common.ConfigurationSource;
import com.github._1c_syntax.mdclasses.mdo.support.MDOModule;
import com.github._1c_syntax.mdclasses.mdo.support.MDOType;
import com.github._1c_syntax.mdclasses.mdo.support.ModuleType;
import com.github._1c_syntax.mdclasses.unmarshal.wrapper.DesignerMDO;
import com.github._1c_syntax.mdclasses.utils.MDOPathUtils;
import com.github._1c_syntax.mdclasses.utils.MDOUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.io.FilenameUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Базовый класс объектов метаданных, имеющих модули с исходным кодом
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
public abstract class AbstractMDObjectBSL extends AbstractMDObjectBase {

  private static final byte[] PROTECTED_FILE_HEADER = new byte[]{-1, -1, -1, 127};
  /**
   * Список модулей объекта
   */
  private List<MDOModule> modules = Collections.emptyList();
  private List<MDOModule> protectedModules = Collections.emptyList();

  protected AbstractMDObjectBSL(DesignerMDO designerMDO) {
    super(designerMDO);
  }

  @Override
  public void supplement() {
    super.supplement();
    MDOPathUtils.getMDOTypeFolderByMDOPath(path, getType()).ifPresent(this::computeAndSetModules);
  }

  @Override
  public void supplement(AbstractMDObjectBase parent) {
    super.supplement(parent);
    MDOPathUtils.getMDOTypeFolderByMDOPath(parent.getPath(), parent.getType())
      .flatMap(folder -> MDOPathUtils.getChildrenFolder(parent.getName(), folder, getType()))
      .ifPresent(this::computeAndSetModules);
  }

  private void computeAndSetModules(Path folder) {
    var moduleTypes = MDOUtils.getModuleTypesForMdoTypes().getOrDefault(getType(), Collections.emptySet());
    if (moduleTypes.isEmpty()) {
      return;
    }

    var configurationSource = MDOUtils.getConfigurationSourceByMDOPath(path);
    var mdoName = (getType() == MDOType.CONFIGURATION) ? "" : getName();
    List<MDOModule> mdoModules = new ArrayList<>();
    List<MDOModule> mdoProtectedModules = new ArrayList<>();
    moduleTypes.forEach((ModuleType moduleType) ->
      MDOPathUtils.getModulePath(configurationSource, folder, mdoName, moduleType)
        .ifPresent((Path modulePath) -> {
          var mdoModule = getMdoModule(configurationSource, moduleType, modulePath);
          mdoModule.ifPresent(module -> {
            if (module.isProtected()){
              mdoProtectedModules.add(module);
            } else {
              mdoModules.add(module);
            }
          });
        }));
    setModules(mdoModules);
    setProtectedModules(mdoProtectedModules);
  }

  private Optional<MDOModule> getMdoModule(ConfigurationSource configurationSource, ModuleType moduleType, Path modulePath) {
    final var modulePathExists = modulePath.toFile().exists();
    var protectedModulePath = computeIsProtected(modulePath, modulePathExists, configurationSource);
    var isProtected = protectedModulePath.isPresent();
    if (modulePathExists) {
      return Optional.of(new MDOModule(moduleType, modulePath.toUri(), this, isProtected));
    } else if (isProtected) {
      return Optional.of(new MDOModule(moduleType, protectedModulePath.get().toUri(), this, true));
    }
    return Optional.empty();
  }

  private Optional<Path> computeIsProtected(Path modulePath, boolean modulePathExists, ConfigurationSource configurationSource) {
    switch (configurationSource) {
      case EDT:
        return computeIsProtectedForEDT(modulePath, modulePathExists);
      case DESIGNER:
        return computeIsProtectedForDesigner(modulePath, modulePathExists);
      default:
        break;
    }
    return Optional.empty();
  }

  private static Optional<Path> computeIsProtectedForEDT(Path modulePath, boolean modulePathExists) {
    if (modulePathExists) {
      byte[] bytes = new byte[PROTECTED_FILE_HEADER.length];

      try (FileInputStream fis = new FileInputStream(modulePath.toFile())) {
        var count = fis.read(bytes);
        if (count == PROTECTED_FILE_HEADER.length && Arrays.equals(bytes, PROTECTED_FILE_HEADER)) {
          return Optional.of(modulePath);
        }
      } catch (IOException e) {
        // ошибка чтения в данном случае неважна
      }
    }
    return Optional.empty();
  }

  private static Optional<Path> computeIsProtectedForDesigner(Path modulePath, boolean modulePathExists) {
    if (!modulePathExists) {
      final var filePath = modulePath.toFile().getPath();
      final var protectedPath = Paths.get(FilenameUtils.removeExtension(filePath) + ".bin");
      if (protectedPath.toFile().exists()) {
        return Optional.of(protectedPath);
      }
    }
    return Optional.empty();
  }
}
