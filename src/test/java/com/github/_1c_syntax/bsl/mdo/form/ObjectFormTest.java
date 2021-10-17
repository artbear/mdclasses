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
package com.github._1c_syntax.bsl.mdo.form;

import com.github._1c_syntax.bsl.mdo.DataProcessor;
import com.github._1c_syntax.bsl.mdo.form.item.UnknownFormItem;
import com.github._1c_syntax.bsl.test_utils.AbstractMDObjectTest;
import com.github._1c_syntax.mdclasses.utils.MDOFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectFormTest extends AbstractMDObjectTest<ObjectForm> {
  private static final String BASE_PATH = "src/test/resources/metadata/formdata/new/originalItems";

  ObjectFormTest() {
    super(ObjectForm.class);
  }

  @Test
  void testObjectForm() {
    var pathToObject = Path.of(BASE_PATH, "DataProcessors/ИследованиеФормы.xml");
    var dataProcessor = getDataProcessor(pathToObject);

    var objectForm = dataProcessor.getForms().get(0);
    var data = objectForm.getData();

    assertThat(data.getChildren()).hasSize(1);
    assertThat(data.getPlainChildren()).isNotEmpty()
      .noneMatch(baseFormItem -> baseFormItem instanceof UnknownFormItem);
    assertThat(data.getAttributes()).hasSize(16);
    assertThat(data.getCommands()).hasSize(1);
  }

  @Test
  void testCommonForm() {
    var pathToObject = Path.of(BASE_PATH, "CommonForms/ОбщаяФорма.xml");
    var commonForm = getCommonForm(pathToObject);
    var data = commonForm.getData();
    assertThat(data.getChildren()).hasSize(1);
  }

  private static DataProcessor getDataProcessor(Path path) {
    var mdo = MDOFactory.readMDObject(path);
    assertThat(mdo).isPresent();
    return DataProcessor.class.cast(mdo.get().buildMDObject());
  }

  private static CommonForm getCommonForm(Path path) {
    var mdo = MDOFactory.readMDObject(path);
    assertThat(mdo).isPresent();
    return CommonForm.class.cast(mdo.get().buildMDObject());
  }

}