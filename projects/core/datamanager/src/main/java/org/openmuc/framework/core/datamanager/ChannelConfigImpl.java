/*
 * Copyright 2011-15 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openmuc.framework.core.datamanager;

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.ChannelState;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;

public final class ChannelConfigImpl implements ChannelConfig, LogChannel {

    String id;
    String channelAddress = null;
    String description = null;
    String unit = null;
    ValueType valueType = null;
    Integer valueTypeLength = null;
    Double scalingFactor = null;
    Double valueOffset = null;
    Boolean listening = null;
    Integer samplingInterval = null;
    Integer samplingTimeOffset = null;
    String samplingGroup = null;
    Integer loggingInterval = null;
    Integer loggingTimeOffset = null;
    Boolean disabled = null;
    List<ServerMapping> serverMappings = null;

    ChannelImpl channel;
    DeviceConfigImpl deviceParent;

    ChannelState state;

    ChannelConfigImpl(String id, DeviceConfigImpl deviceParent) {
        this.id = id;
        this.deviceParent = deviceParent;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) throws IdCollisionException {
        if (id == null) {
            throw new IllegalArgumentException("The channel ID may not be null");
        }
        ChannelConfigImpl.checkIdSyntax(id);

        if (deviceParent.driverParent.rootConfigParent.channelConfigsById.containsKey(id)) {
            throw new IdCollisionException("Collision with channel ID:" + id);
        }

        deviceParent.channelConfigsById.put(id, deviceParent.channelConfigsById.remove(this.id));
        deviceParent.driverParent.rootConfigParent.channelConfigsById
                .put(id, deviceParent.driverParent.rootConfigParent.channelConfigsById.remove(this.id));

        this.id = id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getChannelAddress() {
        return channelAddress;
    }

    @Override
    public void setChannelAddress(String address) {
        channelAddress = address;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    @Override
    public Integer getValueTypeLength() {
        return valueTypeLength;
    }

    @Override
    public void setValueTypeLength(Integer length) {
        valueTypeLength = length;
    }

    @Override
    public Double getScalingFactor() {
        return scalingFactor;
    }

    @Override
    public void setScalingFactor(Double factor) {
        scalingFactor = factor;
    }

    @Override
    public Double getValueOffset() {
        return valueOffset;
    }

    @Override
    public void setValueOffset(Double offset) {
        valueOffset = offset;
    }

    @Override
    public Boolean isListening() {
        return listening;
    }

    @Override
    public void setListening(Boolean listening) {
        if (samplingInterval != null && listening != null && listening && samplingInterval > 0) {
            throw new IllegalStateException("Listening may not be enabled while sampling is enabled.");
        }
        this.listening = listening;
    }

    @Override
    public Integer getSamplingInterval() {
        return samplingInterval;
    }

    @Override
    public void setSamplingInterval(Integer samplingInterval) {
        if (listening != null && samplingInterval != null && listening && samplingInterval > 0) {
            throw new IllegalStateException("Sampling may not be enabled while listening is enabled.");
        }
        this.samplingInterval = samplingInterval;
    }

    @Override
    public Integer getSamplingTimeOffset() {
        return samplingTimeOffset;
    }

    @Override
    public void setSamplingTimeOffset(Integer samplingTimeOffset) {
        if (samplingTimeOffset != null && samplingTimeOffset < 0) {
            throw new IllegalArgumentException("The sampling time offset may not be negative.");
        }
        this.samplingTimeOffset = samplingTimeOffset;
    }

    @Override
    public String getSamplingGroup() {
        return samplingGroup;
    }

    @Override
    public void setSamplingGroup(String group) {
        samplingGroup = group;
    }

    @Override
    public Integer getLoggingInterval() {
        return loggingInterval;
    }

    @Override
    public void setLoggingInterval(Integer loggingInterval) {
        this.loggingInterval = loggingInterval;
    }

    @Override
    public Integer getLoggingTimeOffset() {
        return loggingTimeOffset;
    }

    @Override
    public void setLoggingTimeOffset(Integer loggingTimeOffset) {
        if (loggingTimeOffset != null && loggingTimeOffset < 0) {
            throw new IllegalArgumentException("The logging time offset may not be negative.");
        }
        this.loggingTimeOffset = loggingTimeOffset;
    }

    @Override
    public Boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public void delete() {
        deviceParent.channelConfigsById.remove(id);
        clear();
    }

    @Override
    public List<ServerMapping> getServerMappings() {
        if (serverMappings != null) {
            return this.serverMappings;
        } else {
            return new ArrayList<ServerMapping>();
        }
    }

    void clear() {
        deviceParent.driverParent.rootConfigParent.channelConfigsById.remove(id);
        deviceParent = null;
    }

    @Override
    public DeviceConfig getDevice() {
        return deviceParent;
    }

    static void addChannelFromDomNode(Node channelConfigNode, DeviceConfig parentConfig) throws ParseException {

        String id = ChannelConfigImpl.getAttributeValue(channelConfigNode, "id");
        if (id == null) {
            throw new ParseException("channel has no id attribute");
        }

        ChannelConfigImpl config;

        try {
            config = (ChannelConfigImpl) parentConfig.addChannel(id);
        } catch (Exception e) {
            throw new ParseException(e);
        }

        NodeList channelChildren = channelConfigNode.getChildNodes();

        try {
            for (int i = 0; i < channelChildren.getLength(); i++) {
                Node childNode = channelChildren.item(i);
                String childName = childNode.getNodeName();

                if (childName.equals("#text")) {
                    continue;
                } else if (childName.equals("description")) {
                    config.setDescription(childNode.getTextContent());
                } else if (childName.equals("channelAddress")) {
                    config.setChannelAddress(childNode.getTextContent());
                } else if (childName.equals("serverMapping")) {
                    NamedNodeMap attributes = childNode.getAttributes();
                    Node nameAttribute = attributes.getNamedItem("id");

                    if (nameAttribute != null) {
                        config.addServerMapping(new ServerMapping(nameAttribute.getTextContent(), childNode.getTextContent()));
                    } else {
                        throw new ParseException("No id attribute specified for serverMapping.");
                    }
                } else if (childName.equals("unit")) {
                    config.setUnit(childNode.getTextContent());
                } else if (childName.equals("valueType")) {
                    String valueTypeString = childNode.getTextContent().toUpperCase();

                    try {
                        config.valueType = ValueType.valueOf(valueTypeString);
                    } catch (IllegalArgumentException e) {
                        throw new ParseException("found unknown channel value type:" + valueTypeString);
                    }

                    if (config.valueType == ValueType.BYTE_ARRAY || config.valueType == ValueType.STRING) {
                        String valueTypeLengthString = getAttributeValue(childNode, "length");
                        if (valueTypeLengthString == null) {
                            throw new ParseException("length of " + config.valueType.toString() + " value type was not specified");
                        }
                        config.valueTypeLength = timeStringToMillis(valueTypeLengthString);
                    }

                } else if (childName.equals("scalingFactor")) {
                    config.setScalingFactor(Double.parseDouble(childNode.getTextContent()));
                } else if (childName.equals("valueOffset")) {
                    config.setValueOffset(Double.parseDouble(childNode.getTextContent()));
                } else if (childName.equals("listening")) {
                    String listeningString = childNode.getTextContent().toLowerCase();
                    if (listeningString.equals("true")) {
                        config.setListening(true);
                    } else if (listeningString.equals("false")) {
                        config.setListening(false);
                    } else {
                        throw new ParseException("\"listening\" tag contains neither \"true\" nor \"false\"");
                    }
                } else if (childName.equals("samplingInterval")) {
                    config.setSamplingInterval(timeStringToMillis(childNode.getTextContent()));
                } else if (childName.equals("samplingTimeOffset")) {
                    config.setSamplingTimeOffset(timeStringToMillis(childNode.getTextContent()));
                } else if (childName.equals("samplingGroup")) {
                    config.setSamplingGroup(childNode.getTextContent());
                } else if (childName.equals("loggingInterval")) {
                    config.setLoggingInterval(timeStringToMillis(childNode.getTextContent()));
                } else if (childName.equals("loggingTimeOffset")) {
                    config.setLoggingTimeOffset(timeStringToMillis(childNode.getTextContent()));
                } else if (childName.equals("disabled")) {
                    String disabledString = childNode.getTextContent().toLowerCase();
                    if (disabledString.equals("true")) {
                        config.setDisabled(true);
                    } else if (disabledString.equals("false")) {
                        config.setDisabled(false);
                    } else {
                        throw new ParseException("\"disabled\" tag contains neither \"true\" nor \"false\"");
                    }
                } else {
                    throw new ParseException("found unknown tag:" + childName);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ParseException(e);
        } catch (IllegalStateException e) {
            throw new ParseException(e);
        }
    }

    Element getDomElement(Document document) {
        Element parentElement = document.createElement("channel");
        parentElement.setAttribute("id", id);

        Element childElement;

        if (description != null) {
            childElement = document.createElement("description");
            childElement.setTextContent(description);
            parentElement.appendChild(childElement);
        }

        if (channelAddress != null) {
            childElement = document.createElement("channelAddress");
            childElement.setTextContent(channelAddress);
            parentElement.appendChild(childElement);
        }

        if (serverMappings != null) {
            for (ServerMapping serverMapping : serverMappings) {
                childElement = document.createElement("serverMapping");
                childElement.setAttribute("id", serverMapping.getId());
                childElement.setTextContent(serverMapping.getServerAddress());
                parentElement.appendChild(childElement);
            }
        }

        if (unit != null) {
            childElement = document.createElement("unit");
            childElement.setTextContent(unit);
            parentElement.appendChild(childElement);
        }

        if (valueType != null) {
            childElement = document.createElement("valueType");
            childElement.setTextContent(valueType.toString());

            if (valueType == ValueType.BYTE_ARRAY || valueType == ValueType.STRING) {
                childElement.setAttribute("length", valueTypeLength.toString());
            }
            parentElement.appendChild(childElement);
        }

        if (scalingFactor != null) {
            childElement = document.createElement("scalingFactor");
            childElement.setTextContent(Double.toString(scalingFactor));
            parentElement.appendChild(childElement);
        }

        if (valueOffset != null) {
            childElement = document.createElement("valueOffset");
            childElement.setTextContent(Double.toString(valueOffset));
            parentElement.appendChild(childElement);
        }

        if (listening != null) {
            childElement = document.createElement("listening");
            if (listening) {
                childElement.setTextContent("true");
            } else {
                childElement.setTextContent("false");
            }
            parentElement.appendChild(childElement);
        }

        if (samplingInterval != null) {
            childElement = document.createElement("samplingInterval");
            childElement.setTextContent(millisToTimeString(samplingInterval));
            parentElement.appendChild(childElement);
        }

        if (samplingTimeOffset != null) {
            childElement = document.createElement("samplingTimeOffset");
            childElement.setTextContent(millisToTimeString(samplingTimeOffset));
            parentElement.appendChild(childElement);
        }

        if (samplingGroup != null) {
            childElement = document.createElement("samplingGroup");
            childElement.setTextContent(samplingGroup);
            parentElement.appendChild(childElement);
        }

        if (loggingInterval != null) {
            childElement = document.createElement("loggingInterval");
            childElement.setTextContent(millisToTimeString(loggingInterval));
            parentElement.appendChild(childElement);
        }

        if (loggingTimeOffset != null) {
            childElement = document.createElement("loggingTimeOffset");
            childElement.setTextContent(millisToTimeString(loggingTimeOffset));
            parentElement.appendChild(childElement);
        }

        if (disabled != null) {
            childElement = document.createElement("disabled");
            if (disabled) {
                childElement.setTextContent("true");
            } else {
                childElement.setTextContent("false");
            }
            parentElement.appendChild(childElement);
        }

        return parentElement;
    }

    ChannelConfigImpl clone(DeviceConfigImpl clonedParentConfig) {
        ChannelConfigImpl configClone = new ChannelConfigImpl(id, clonedParentConfig);

        configClone.description = description;
        configClone.channelAddress = channelAddress;
        configClone.serverMappings = serverMappings;
        configClone.unit = unit;
        configClone.valueType = valueType;
        configClone.valueTypeLength = valueTypeLength;
        configClone.scalingFactor = scalingFactor;
        configClone.valueOffset = valueOffset;
        configClone.listening = listening;
        configClone.samplingInterval = samplingInterval;
        configClone.samplingTimeOffset = samplingTimeOffset;
        configClone.samplingGroup = samplingGroup;
        configClone.loggingInterval = loggingInterval;
        configClone.loggingTimeOffset = loggingTimeOffset;
        configClone.disabled = disabled;

        return configClone;
    }

    ChannelConfigImpl cloneWithDefaults(DeviceConfigImpl clonedParentConfig) {
        ChannelConfigImpl configClone = new ChannelConfigImpl(id, clonedParentConfig);

        if (description == null) {
            configClone.description = ChannelConfig.DESCRIPTION_DEFAULT;
        } else {
            configClone.description = description;
        }

        if (channelAddress == null) {
            configClone.channelAddress = CHANNEL_ADDRESS_DEFAULT;
        } else {
            configClone.channelAddress = channelAddress;
        }

        if (serverMappings == null) {
            configClone.serverMappings = new ArrayList<ServerMapping>();
        } else {
            configClone.serverMappings = serverMappings;
        }

        if (unit == null) {
            configClone.unit = ChannelConfig.UNIT_DEFAULT;
        } else {
            configClone.unit = unit;
        }

        if (valueType == null) {
            configClone.valueType = ChannelConfig.VALUE_TYPE_DEFAULT;
        } else {
            configClone.valueType = valueType;
        }

        if (valueTypeLength == null) {
            if (valueType == ValueType.DOUBLE) {
                configClone.valueTypeLength = 8;
            } else if (valueType == ValueType.BYTE_ARRAY) {
                configClone.valueTypeLength = ChannelConfig.BYTE_ARRAY_SIZE_DEFAULT;
            } else if (valueType == ValueType.STRING) {
                configClone.valueTypeLength = ChannelConfig.STRING_SIZE_DEFAULT;
            } else if (valueType == ValueType.BYTE) {
                configClone.valueTypeLength = 1;
            } else if (valueType == ValueType.BYTE) {
                configClone.valueTypeLength = 1;
            } else if (valueType == ValueType.FLOAT) {
                configClone.valueTypeLength = 4;
            } else if (valueType == ValueType.SHORT) {
                configClone.valueTypeLength = 2;
            } else if (valueType == ValueType.INTEGER) {
                configClone.valueTypeLength = 4;
            } else if (valueType == ValueType.LONG) {
                configClone.valueTypeLength = 8;
            } else if (valueType == ValueType.BOOLEAN) {
                configClone.valueTypeLength = 1;
            }
        } else {
            configClone.valueTypeLength = valueTypeLength;
        }

        configClone.scalingFactor = scalingFactor;
        configClone.valueOffset = valueOffset;

        if (listening == null) {
            configClone.listening = ChannelConfig.LISTENING_DEFAULT;
        } else {
            configClone.listening = listening;
        }

        if (samplingInterval == null) {
            configClone.samplingInterval = ChannelConfig.SAMPLING_INTERVAL_DEFAULT;
        } else {
            configClone.samplingInterval = samplingInterval;
        }

        if (samplingTimeOffset == null) {
            configClone.samplingTimeOffset = ChannelConfig.SAMPLING_TIME_OFFSET_DEFAULT;
        } else {
            configClone.samplingTimeOffset = samplingTimeOffset;
        }

        if (samplingGroup == null) {
            configClone.samplingGroup = ChannelConfig.SAMPLING_GROUP_DEFAULT;
        } else {
            configClone.samplingGroup = samplingGroup;
        }

        if (loggingInterval == null) {
            configClone.loggingInterval = ChannelConfig.LOGGING_INTERVAL_DEFAULT;
        } else {
            configClone.loggingInterval = loggingInterval;
        }

        if (loggingTimeOffset == null) {
            configClone.loggingTimeOffset = ChannelConfig.LOGGING_TIME_OFFSET_DEFAULT;
        } else {
            configClone.loggingTimeOffset = loggingTimeOffset;
        }

        if (disabled == null) {
            configClone.disabled = clonedParentConfig.disabled;
        } else {
            if (clonedParentConfig.disabled) {
                configClone.disabled = false;
            } else {
                configClone.disabled = disabled;
            }
        }

        return configClone;
    }

    static String getAttributeValue(Node element, String attributeName) {
        NamedNodeMap attributes = element.getAttributes();

        Node nameAttribute = attributes.getNamedItem(attributeName);

        if (nameAttribute == null) {
            return null;
        }
        return nameAttribute.getTextContent();
    }

    static String millisToTimeString(Integer time) {
        if (time == 0) {
            return "0";
        }
        if ((time % 1000) == 0) {
            time /= 1000;
            if ((time % 60) == 0) {
                time /= 60;
                if ((time % 60) == 0) {
                    return time.toString().concat("h");
                }
                return time.toString().concat("m");
            }
            return time.toString().concat("s");
        }
        return time.toString().concat("ms");
    }

    static int timeStringToMillis(String timeString) throws ParseException {
        try {

            char lastChar = timeString.charAt(timeString.length() - 1);

            if (Character.isDigit(lastChar)) {
                return Integer.parseInt(timeString);
            }

            switch (lastChar) {
                case 's':
                    if (timeString.charAt(timeString.length() - 2) == 'm') {
                        return Integer.parseInt(timeString.substring(0, timeString.length() - 2));
                    }
                    return Integer.parseInt(timeString.substring(0, timeString.length() - 1)) * 1000;
                case 'm':
                    return Integer.parseInt(timeString.substring(0, timeString.length() - 1)) * 60000;
                case 'h':
                    return Integer.parseInt(timeString.substring(0, timeString.length() - 1)) * 3600000;
                default:
                    throw new ParseException("unknown time string: " + timeString);
            }

        } catch (NumberFormatException e) {
            throw new ParseException(e);
        }

    }

    static void checkIdSyntax(String id) {
        if (!id.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Invalid ID: \"" + id + "\". An ID may not be the empty string and must contain only ASCII letters, digits, hyphens and underscores.");
        }
    }

    public boolean isSampling() {
        return !disabled && samplingInterval != null && samplingInterval > 0;
    }

    @Override
    public void addServerMapping(ServerMapping serverMapping) {
        if (serverMappings == null) {
            serverMappings = new ArrayList<ServerMapping>();
        }
        serverMappings.add(serverMapping);
    }

    @Override
    public void deleteServerMappings(String id) {
        if (serverMappings != null) {
            List<ServerMapping> newMappings = new ArrayList<ServerMapping>();
            for (ServerMapping serverMapping : serverMappings) {
                if (!serverMapping.getId().equals(id)) {
                    newMappings.add(serverMapping);
                }
            }
            serverMappings = newMappings;
        }
    }
}
