/*
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.monitor.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import com.hazelcast.monitor.client.event.*;
import com.hazelcast.monitor.client.handler.InstanceCreatedHandler;
import com.hazelcast.monitor.client.handler.InstanceDestroyedHandler;
import com.hazelcast.monitor.client.handler.MemberEventHandler;

import java.util.*;

public class ClusterWidgets {
    int clusterId;
    Tree clusterTree;
    String clusterName;
    TreeItem memberTreeItem;
    public HorizontalSplitPanel mainPanel;
    //    Map<ChangeEventType, Widget> activeWidgets = new HashMap<ChangeEventType, Widget>();
    Map<InstanceType, InstanceWidgets> itemMap = new HashMap<InstanceType, InstanceWidgets>();
    Map<ChangeEventType, List<MonitoringPanel>> panels = new HashMap<ChangeEventType, List<MonitoringPanel>>();

    final private HazelcastMonitor hazelcastMonitor;
    final private ClusterView cv;
    private Map<String, List<ChangeEventType>> registeredChangeEvents = new HashMap<String, List<ChangeEventType>>();

    public ClusterWidgets(HazelcastMonitor hazelcastMonitor, ClusterView cv) {
        this.hazelcastMonitor = hazelcastMonitor;
        this.cv = cv;
        this.clusterId = cv.getId();
        Tree tree = addTreeItems(cv);
        this.clusterTree = tree;
    }

    public Map<ChangeEventType, List<MonitoringPanel>> getPanels() {
        return panels;
    }

    public TreeItem getMemberTreeItem() {
        return memberTreeItem;
    }

    public Map<InstanceType, InstanceWidgets> getItemMap() {
        return itemMap;
    }

    public void handle(ChangeEvent changeEvent) {
//        System.out.println("Handling event: " + changeEvent);
        if (changeEvent instanceof InstanceCreated) {
            new InstanceCreatedHandler(this).handle(changeEvent);
        } else if (changeEvent instanceof InstanceDestroyed) {
            new InstanceDestroyedHandler(this).handle(changeEvent);
        } else if (changeEvent instanceof MemberEvent) {
            new MemberEventHandler(this).handle(changeEvent);
        } else {
            List<MonitoringPanel> list = panels.get(changeEvent.getChangeEventType());
            if (list == null || list.isEmpty()) {
                System.out.println("Unknown event:" + changeEvent.getChangeEventType());
                return;
            }
            for (Iterator<MonitoringPanel> iterator = list.iterator(); iterator.hasNext();) {
                MonitoringPanel monitoringPanel = iterator.next();
                monitoringPanel.handle(changeEvent);
            }
        }
    }

    public void register(MonitoringPanel panel) {
        boolean registered = panel.register(this);
        if (registered) {
            VerticalPanel rightPanel = (VerticalPanel) mainPanel.getRightWidget();
            rightPanel.add(panel.getDisclosurePanel());
        }
    }

    public void deRegister(MonitoringPanel panel) {
        panel.deRegister(this);
        VerticalPanel rightPanel = (VerticalPanel) mainPanel.getRightWidget();
        rightPanel.remove(panel.getDisclosurePanel());
    }

    public void deRegisterAll() {
        Set<ChangeEventType> s = panels.keySet();
        for (Iterator<ChangeEventType> iterator = s.iterator(); iterator.hasNext();) {
            ChangeEventType key = iterator.next();
            List<MonitoringPanel> list = panels.get(key);
            while (list.size() > 0) {
                MonitoringPanel panel = list.get(0);
                deRegister(panel);
            }
            panels.remove(key);
        }
    }

    private Tree addTreeItems(ClusterView cv) {
        Tree tree = new Tree();
        memberTreeItem = addTreeItem(tree, "Members", cv.getMembers(), clusterId, null);
        itemMap.put(InstanceType.MAP, new InstanceWidgets(InstanceType.MAP,
                addTreeItem(tree, "Maps", cv.getMaps(), clusterId, InstanceType.MAP)));
        itemMap.put(InstanceType.QUEUE, new InstanceWidgets(InstanceType.QUEUE,
                addTreeItem(tree, "Queues", cv.getQs(), clusterId, InstanceType.QUEUE)));
        itemMap.put(InstanceType.LIST, new InstanceWidgets(InstanceType.LIST,
                addTreeItem(tree, "Lists", cv.getLists(), clusterId, InstanceType.LIST)));
        itemMap.put(InstanceType.SET, new InstanceWidgets(InstanceType.SET,
                addTreeItem(tree, "Sets", cv.getSets(), clusterId, InstanceType.SET)));
        itemMap.put(InstanceType.TOPIC, new InstanceWidgets(InstanceType.TOPIC,
                addTreeItem(tree, "Topics", cv.getTopics(), clusterId, InstanceType.TOPIC)));
        itemMap.put(InstanceType.MULTIMAP, new InstanceWidgets(InstanceType.MULTIMAP,
                addTreeItem(tree, "MultiMaps", cv.getMultiMaps(), clusterId, InstanceType.MULTIMAP)));
        itemMap.put(InstanceType.LOCK, new InstanceWidgets(InstanceType.LOCK,
                addTreeItem(tree, "Locks", cv.getLocks(), clusterId, InstanceType.LOCK)));
        return tree;
    }

    private TreeItem addTreeItem(Tree tree, String headerName, List<String> itemList, int clusterId, InstanceType type) {
        TreeItem treeItem = new TreeItem(headerName);
        addItems(itemList, treeItem, clusterId, type);
        tree.addItem(treeItem);
        return treeItem;
    }

    private void addItems(List<String> itemList, TreeItem treeItem, int clusterId, InstanceType type) {
        for (Iterator<String> iterator = itemList.iterator(); iterator.hasNext();) {
            String string = iterator.next();
            Widget link = getInstanceLink(type, string);
            treeItem.addItem(link);
        }
    }

    public Anchor getInstanceLink(InstanceType type, String name) {
        final String token =
                "clusterId=" + clusterId +
                        "&type=" + ((type == null) ? "MEMBER" : type) +
                        "&name=" + name;
//        Hyperlink link = new Hyperlink(name, token);
        Anchor anchor = new Anchor(name);
        anchor.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                hazelcastMonitor.onValueChange(token);
            }
        });
        return anchor;
    }

    public List<ChangeEventType> getRegisteredChangeEvents(String name) {
        List<ChangeEventType> list = registeredChangeEvents.get(name);
        if(list == null){
            registeredChangeEvents.put(name, new ArrayList<ChangeEventType>());
        }
        return registeredChangeEvents.get(name);
    }
}