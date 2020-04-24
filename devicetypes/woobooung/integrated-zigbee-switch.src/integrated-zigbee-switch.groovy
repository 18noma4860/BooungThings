/*
 *  Integrated ZigBee Switch
 * 
 *  Copyright 2020 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  author : woobooung@gmail.com
 */
public static String version() { return "v0.0.1.20200425" }
/*
 *   2020/04/25 >>> v0.0.1.20200425 - Initialize : Bandi ZigBee Switch, Zemi ZigBee Switch
 */
metadata {
    definition(name: "Integrated ZigBee Switch", namespace: "WooBooung", author: "Booung", ocfDeviceType: "oic.d.switch", vid: "generic-switch") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Health Check"
        capability "Switch"

        command "childOn", ["string"]
        command "childOff", ["string"]

        // Bandi ZigBee Multi Switch
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0100", inClusters: "0000, 000A, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TZ3000_pdevogdj", model: "TS0003", deviceJoinName: "Bandi Zigbee Switch 1"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0100", inClusters: "0000, 000A, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TZ3000_tas0zemd", model: "TS0002", deviceJoinName: "Bandi Zigbee Switch 1"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0100", inClusters: "0000, 000A, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TZ3000_ddg0cycp", model: "TS0001", deviceJoinName: "Bandi Zigbee Switch"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0100", inClusters: "0000, 000A, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYZB01_pdevogdj", model: "TS0003", deviceJoinName: "Bandi Zigbee Switch 1"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0100", inClusters: "0000, 000A, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYZB01_tas0zemd", model: "TS0002", deviceJoinName: "Bandi Zigbee Switch 1"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0100", inClusters: "0000, 000A, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYZB01_ddg0cycp", model: "TS0001", deviceJoinName: "Bandi Zigbee Switch"

        // Zemi ZigBee Multi Switch
        fingerprint endpointId: "10", profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", manufacturer: "Feibit Inc co.", model: "FB56+ZSW1GKJ2.7", deviceJoinName: "Zemi Zigbee Switch"
        fingerprint endpointId: "0B", profileId: "C05E", inClusters: "0000, 0004, 0003, 0006, 0005, 1000, 0008", outClusters: "0019", manufacturer: "FeiBit", model: "FNB56-ZSW02LX2.0", deviceJoinName: "Zemi Zigbee Switch 1"
        fingerprint endpointId: "01", profileId: "C05E", inClusters: "0000, 0004, 0003, 0006, 0005, 1000, 0008", outClusters: "0019", manufacturer: "FeiBit", model: "FNB56-ZSW03LX2.0", deviceJoinName: "Zemi Zigbee Switch 1"
    }

    preferences {
        input type: "paragraph", element: "paragraph", title: "Version", description: version(), displayDuringSetup: false
    }

    // simulator metadata
    simulator {
        // status messages
        status "on": "on/off: 1"
        status "off": "on/off: 0"

        // reply messages
        reply "zcl on-off on": "on/off: 1"
        reply "zcl on-off off": "on/off: 0"
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
            }
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        main "switch"
        details(["switch", "refresh"])
    }
}

def installed() {
    log.debug "installed()"
    if (parent) {
        setDeviceType("Child Switch Health")
    } else {
        def model = device.getDataValue("model")
        if (model == "TS0001" || model == "FB56+ZSW1GKJ2.7") {
            // for 1 gang switch - ST Official local dth
            setDeviceType("ZigBee Switch")
        } else {
            // for multi switch, cloud device
            createChildDevices()
        }
        updateDataValue("onOff", "catchall")
        refresh()
    }
}

def updated() {
    log.debug "updated()"
    updateDataValue("onOff", "catchall")
    refresh()
}

def parse(String description) {
    Map eventMap = zigbee.getEvent(description)
    Map eventDescMap = zigbee.parseDescriptionAsMap(description)

    if (!eventMap && eventDescMap) {
        eventMap = [:]
        if (eventDescMap?.clusterId == zigbee.ONOFF_CLUSTER) {
            eventMap[name] = "switch"
            eventMap[value] = eventDescMap?.value
        }
    }

    if (eventMap) {
        log.debug "eventMap $eventMap"
        log.debug "eventDescMap $eventDescMap"
        def endpointId = device.getDataValue("endpointId")

        if (eventDescMap?.sourceEndpoint == endpointId) {
            log.debug "parse- sendEvent parent $eventDescMap.sourceEndpoint"
            sendEvent(eventMap)
        } else {
            log.debug "parse- sendEvent child  $eventDescMap.sourceEndpoint"
            def childDevice = childDevices.find {
                it.deviceNetworkId == "$device.deviceNetworkId:${eventDescMap.sourceEndpoint}"
            }
            if (childDevice) {
                childDevice.sendEvent(eventMap)
            } else {
                log.debug "Child device: $device.deviceNetworkId:${eventDescMap.sourceEndpoint} was not found"
            }
        }
    }
}

private getChildCount() {
    def model = device.getDataValue("model")

    switch (model) {
        case 'TS0003' : return 3
        case 'TS0002' : return 2
        case 'FNB56-ZSW03LX2.0' : return 3
        case 'FNB56-ZSW02LX2.0' : return 2
        default : return 2
    }
}

private void createChildDevices() {
    log.debug("createChildDevices of $device.deviceNetworkId")
    def x = getChildCount()
    def model = device.getDataValue("model")
    def endpointId = device.getDataValue("endpointId")
    def endpointInt = zigbee.convertHexToInt(endpointId)

    for (i in 1..x - 1) {
        def endpointHexString = zigbee.convertToHexString(endpointInt + i, 2).toUpperCase()

        def childDevice = childDevices.find {
            it.deviceNetworkId == "$device.deviceNetworkId:${endpointHexString}"
        }
        if (!childDevice) {
            addChildDevice("$device.type", "$device.deviceNetworkId:${endpointHexString}", device.hubId,
                           [completedSetup: true, label: "${device.displayName[0..-2]}${i + 1}", isComponent: false])
        } else {
        	log.debug("createChildDevices: skip - $device.deviceNetworkId:${endpointHexString}")
        }
    }
}

private getChildEndpoint(String dni) {
    dni.split(":")[-1] as String
}

def on() {
    log.debug("on")
    zigbee.on()
}

def off() {
    log.debug("off")
    zigbee.off()
}

def childOn(String dni) {
    log.debug("child on ${dni}")
    def childEndpoint = getChildEndpoint(dni)
    zigbee.command(zigbee.ONOFF_CLUSTER, 0x01, "", [destEndpoint: childEndpoint])
}

def childOff(String dni) {
    log.debug("child off ${dni}")
    def childEndpoint = getChildEndpoint(dni)
    zigbee.command(zigbee.ONOFF_CLUSTER, 0x00, "", [destEndpoint: childEndpoint])
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    return refresh()
}

def refresh() {
    def cmds = zigbee.onOffRefresh()
    def x = getChildCount()
    def model = device.getDataValue("model")
    def endpointId = device.getDataValue("endpointId")
    def endpointInt = zigbee.convertHexToInt(endpointId)

    for (i in 1..x - 1) {
        def endpointValue = endpointInt + i
        cmds += zigbee.readAttribute(zigbee.ONOFF_CLUSTER, 0x0000, [destEndpoint: endpointValue])
    }

    return cmds
}

def poll() {
    refresh()
}

def healthPoll() {
    log.debug "healthPoll()"
    def cmds = refresh()
    cmds.each { sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

def configureHealthCheck() {
    Integer hcIntervalMinutes = 12
    if (!state.hasConfiguredHealthCheck) {
        log.debug "Configuring Health Check, Reporting"
        unschedule("healthPoll")
        runEvery5Minutes("healthPoll")
        def healthEvent = [name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
        // Device-Watch allows 2 check-in misses from device
        sendEvent(healthEvent)
        childDevices.each {
            it.sendEvent(healthEvent)
        }
        state.hasConfiguredHealthCheck = true
    }
}

def configure() {
    log.debug "configure()"
    configureHealthCheck()

    //other devices supported by this DTH in the future
    def cmds = zigbee.onOffConfig(0, 120)
    def x = getChildCount()
    def model = device.getDataValue("model")
    def endpointId = device.getDataValue("endpointId")
    def endpointInt = zigbee.convertHexToInt(endpointId)

    for (i in 1..x - 1) {
        def endpointValue = endpointInt + i
        cmds += zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, 0x10, 0, 120, null, [destEndpoint: endpointValue])
    }
    cmds += refresh()
    return cmds
}
