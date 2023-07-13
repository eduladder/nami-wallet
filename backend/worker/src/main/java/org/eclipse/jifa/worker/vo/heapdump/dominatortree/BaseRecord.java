/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.jifa.worker.vo.heapdump.dominatortree;

import lombok.Data;

@Data
public class BaseRecord {

    private String label;

    private String suffix;

    private int objectId;

    private int objectType;

    private boolean gCRoot;

    private long shallowSize;

    private long retainedSize;

    private double percent;
}
