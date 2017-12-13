/*
 * Copyright (c) 2008-2017 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.gui.components;

import javax.annotation.Nullable;

public interface MetaComponentFactory {

    /**
     * Defines the highest precedence for {@link org.springframework.core.Ordered} platform agents.
     */
    int HIGHEST_PLATFORM_PRECEDENCE = 100;

    /**
     * Defines the lowest precedence for {@link org.springframework.core.Ordered} platform agents.
     */
    int LOWEST_PLATFORM_PRECEDENCE = 1000;

    @Nullable
    Component createComponent(MetaContext context);
}
