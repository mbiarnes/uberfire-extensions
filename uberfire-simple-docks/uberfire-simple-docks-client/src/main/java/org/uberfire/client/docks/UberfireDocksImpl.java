/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.client.docks;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import org.uberfire.client.docks.view.DocksBar;
import org.uberfire.client.docks.view.DocksBars;
import org.uberfire.client.workbench.docks.UberfireDock;
import org.uberfire.client.workbench.docks.UberfireDockPosition;
import org.uberfire.client.workbench.docks.UberfireDockReadyEvent;
import org.uberfire.client.workbench.docks.UberfireDocks;
import org.uberfire.client.workbench.events.PerspectiveChange;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class UberfireDocksImpl implements UberfireDocks {

    public static final String IDE_DOCK = "IDE_DOCK";

    final Map<String, Set<UberfireDock>> docksPerPerspective = new HashMap<String, Set<UberfireDock>>();

    final Map<String, Set<UberfireDockPosition>> disableDocksPerPerspective = new HashMap<String, Set<UberfireDockPosition>>();

    private DocksBars docksBars;

    String currentSelectedPerspective;

    @Inject
    private Event<UberfireDockReadyEvent> dockReadyEvent;

    @Inject
    public UberfireDocksImpl(DocksBars docksBars) {
        this.docksBars = docksBars;
    }

    @Override
    public void setup(DockLayoutPanel rootContainer) {
        docksBars.setup(rootContainer);
        updateDocks();
    }

    @Override
    public void configure(Map<String, String> configurations) {
        if (configurations != null && configurations.get(IDE_DOCK) != null) {
            docksBars.setIDEdock(Boolean.valueOf(configurations.get(IDE_DOCK)));
        }
    }

    @Override
    public void add(UberfireDock... docks) {
        for (UberfireDock dock : docks) {
            if (dock.getAssociatedPerspective() != null) {
                Set<UberfireDock> uberfireDocks = docksPerPerspective.get(dock.getAssociatedPerspective());
                if (uberfireDocks == null) {
                    uberfireDocks = new HashSet<UberfireDock>();
                }
                uberfireDocks.add(dock);
                docksPerPerspective.put(dock.getAssociatedPerspective(), uberfireDocks);
            }
        }
        updateDocks();
    }

    public void perspectiveChangeEvent(@Observes PerspectiveChange perspectiveChange) {
        this.currentSelectedPerspective = perspectiveChange.getIdentifier();
        updateDocks();
        fireEvent();
    }

    protected void fireEvent() {
        dockReadyEvent.fire(new UberfireDockReadyEvent(currentSelectedPerspective));
    }


    @Override
    public void remove(UberfireDock... docks) {
        for (UberfireDock dock : docks) {
            if (dock.getAssociatedPerspective() != null) {
                Set<UberfireDock> uberfireDocks = docksPerPerspective.get(dock.getAssociatedPerspective());
                uberfireDocks.remove(dock);
                docksPerPerspective.put(dock.getAssociatedPerspective(), uberfireDocks);
            }
        }
        updateDocks();
    }

    @Override
    public void expand(UberfireDock dock) {
        if (docksBars.isReady()) {
            docksBars.expand(dock);
        }
    }

    @Override
    public void disable(UberfireDockPosition position, String perspectiveName) {
        addToDisableDocksList(position, perspectiveName);
        disableDock(position);
    }

    private void disableDock(UberfireDockPosition position) {
        if (docksBars.isReady()) {
            docksBars.clearAndCollapse(position);
        }
    }

    @Override
    public void enable(UberfireDockPosition position, String perspectiveName) {
        removeFromDisableDocksList(position, perspectiveName);
        enableDock(position);
    }

    private void enableDock(UberfireDockPosition position) {
        if (docksBars.isReady()) {
            docksBars.clearAndCollapse(position);
            if (currentSelectedPerspective != null) {
                Set<UberfireDock> docks = docksPerPerspective.get(currentSelectedPerspective);
                if (docks != null && !docks.isEmpty()) {
                    for (UberfireDock dock : docks) {
                        if (dock.getDockPosition().equals(position)) {
                            docksBars.addDock(dock);
                        }
                    }
                    docksBars.expand(position);
                }
            }
        }
    }


    void updateDocks() {
        if (docksBars.isReady()) {
            docksBars.clearAndCollapseAllDocks();
            if (currentSelectedPerspective != null) {
                Set<UberfireDock> docks = docksPerPerspective.get(currentSelectedPerspective);
                if (docks != null && !docks.isEmpty()) {
                    for (UberfireDock dock : docks) {
                        docksBars.addDock(dock);
                    }
                    expandAllAvailableDocks();
                }
            }
        }
    }


    private void expandAllAvailableDocks() {
        for (DocksBar docksBar : docksBars.getDocksBars()) {
            if (dockIsEnable(docksBar.getPosition())) {
                docksBars.expand(docksBar);
            }
        }
    }


    private void addToDisableDocksList(UberfireDockPosition position, String perspectiveName) {
        Set<UberfireDockPosition> disableDocks = disableDocksPerPerspective.get(perspectiveName);
        if (disableDocks == null) {
            disableDocks = new HashSet<UberfireDockPosition>();
            disableDocksPerPerspective.put(perspectiveName, disableDocks);
        }
        disableDocks.add(position);
    }


    private void removeFromDisableDocksList(UberfireDockPosition position, String perspectiveName) {
        Set<UberfireDockPosition> disableDocks = disableDocksPerPerspective.get(perspectiveName);
        if (disableDocks != null) {
            disableDocks.remove(position);
        }
    }

    private boolean dockIsEnable(UberfireDockPosition dockPosition) {
        Set<UberfireDockPosition> uberfireDockPositions = disableDocksPerPerspective.get(currentSelectedPerspective);
        if (uberfireDockPositions != null && uberfireDockPositions.contains(dockPosition)) {
            return false;
        }
        return true;
    }
}
