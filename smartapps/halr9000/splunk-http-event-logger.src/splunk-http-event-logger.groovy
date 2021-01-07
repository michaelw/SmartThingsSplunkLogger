/**
* Event Logger For Splunk
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* 01-17-2016 Merged code from rlyons20 to splunk lock devices
* 12-04-2016 Fixed the results so that they not only spit out the results but they still also send it off to splunk like it should
* 02-16-2016 Added the ability for non-ssl/SSL
* 05-18-2016 Added the ability to log to splunk over the lan and
* 10-24-2017 Added the code from Uto to log humidity readings and spelling fixes
*   used adrabkin code fix for the length with local logging
* 08-07-2019 Reformatting, removed HOST header from HTTP payload. See full changelog at https://github.com/halr9000/SmartThingsSplunkLogger
*/
import groovy.json.JsonOutput

definition(
    name: "Splunk HTTP Event Logger",
    namespace: "halr9000",
    author: "Brian Keifer, Jason Hamilton, Hal Rottenberg",
    description: "Log SmartThings events to a Splunk HTTP Event Collector server",
    category: "Convenience",
    iconUrl: "https://cdn.apps.splunk.com/media/public/icons/cdb1e1dc-5a0e-11e4-84b5-0af1e3fac1ba.png",
    iconX2Url: "https://cdn.apps.splunk.com/media/public/icons/cdb1e1dc-5a0e-11e4-84b5-0af1e3fac1ba.png",
    iconX3Url: "https://cdn.apps.splunk.com/media/public/icons/cdb1e1dc-5a0e-11e4-84b5-0af1e3fac1ba.png")

preferences {
    section("Log these presence sensors:") {
        input "presences", "capability.presenceSensor", multiple: true, required: false
    }
    section("Log these switches:") {
        input "switches", "capability.switch", multiple: true, required: false
    }
    section("Log these switch levels:") {
        input "levels", "capability.switchLevel", multiple: true, required: false
    }
    section("Log these motion sensors:") {
        input "motions", "capability.motionSensor", multiple: true, required: false
    }
    section("Log these temperature sensors:") {
        input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Log these humidity sensors:") {
        input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false
    }
    section("Log these contact sensors:") {
        input "contacts", "capability.contactSensor", multiple: true, required: false
    }
    section("Log these alarms:") {
        input "alarms", "capability.alarm", multiple: true, required: false
    }
    section("Log these indicators:") {
        input "indicators", "capability.indicator", multiple: true, required: false
    }
    section("Log these CO detectors:") {
        input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false
    }
    section("Log these smoke detectors:") {
        input "smokedetectors", "capability.smokeDetector", multiple: true, required: false
    }
    section("Log these water detectors:") {
        input "waterdetectors", "capability.waterSensor", multiple: true, required: false
    }
    section("Log these acceleration sensors:") {
        input "accelerations", "capability.accelerationSensor", multiple: true, required: false
    }
    section("Log these energy meters:") {
        input "energymeters", "capability.energyMeter", multiple: true, required: false
    }
    section("Log these music players:") {
        input "musicplayer", "capability.musicPlayer", multiple: true, required: false
    }
    section("Log these power meters:") {
        input "powermeters", "capability.powerMeter", multiple: true, required: false
    }
    section("Log these illuminance sensors:") {
        input "illuminances", "capability.illuminanceMeasurement", multiple: true, required: false
    }
    section("Log these batteries:") {
        input "batteries", "capability.battery", multiple: true, required: false
    }
    section("Log these buttons:") {
        input "button", "capability.button", multiple: true, required: false
    }
    section("Log these voltages:") {
        input "voltage", "capability.voltageMeasurement", multiple: true, required: false
    }
    section("Log these locks:") {
        input "lockDevice", "capability.lock", multiple: true, required: false
    }
    section("Scheduled Device Polling") {
        input "do_device_poll", "boolean", title: "Poll devices every 5 mins?", required: true
    }

    section ("Splunk Server") {
        input "use_local", "boolean", title: "Local Server?", required: true
        input "splunk_host", "text", title: "Splunk Hostname/IP", required: true
        input "splunk_hec_host", "text", title: "Splunk HEC Hostname", required: false
        input "use_ssl", "boolean", title: "Use SSL?", required: true
        input "splunk_port", "number", title: "Splunk Port", required: true
        input "splunk_token", "text", title: "Splunk Authentication Token", required: true
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    doSubscriptions()
    if (do_device_poll) {
        reportStates()
        runEvery5Minutes(reportStates)
    }
}

// Subscribes to the various Events for a device or Location. The specified handlerMethod will be called when the Event is fired.
// subscribe specification: https://docs.smartthings.com/en/latest/ref-docs/smartapp-ref.html#subscribe
def doSubscriptions() {
    subscribe(alarms,"alarm",alarmHandler)
    subscribe(codetectors,"carbonMonoxideDetector",coHandler)
    subscribe(contacts,"contact", contactHandler)
    subscribe(indicators,"indicator", indicatorHandler)
    subscribe(modes,"locationMode", modeHandler)
    subscribe(motions,"motion", motionHandler)
    subscribe(presences,"presence", presenceHandler)
    subscribe(relays,"relaySwitch", relayHandler)
    subscribe(smokedetectors,"smokeDetector",smokeHandler)
    subscribe(switches,"switch", switchHandler)
    subscribe(levels,"level",levelHandler)
    subscribe(temperatures,"temperature", temperatureHandler)
    subscribe(waterdetectors,"water",waterHandler)
    subscribe(location,"location",locationHandler)
    subscribe(accelerations, "acceleration", accelerationHandler)
    subscribe(energymeters, "energy", energyHandler)
    subscribe(musicplayers, "music", musicHandler)
    subscribe(lightSensor,"illuminance",illuminanceHandler)
    subscribe(powermeters,"power",powerHandler)
    subscribe(batteries,"battery", batteryHandler)
    subscribe(button, "button", buttonHandler)
    subscribe(voltageMeasurement, "voltage", voltageHandler)
    subscribe(lockDevice, "lock", lockHandler)
    subscribe(humidities, "humidity", humidityHandler)
}

def reportStates() {
    def devices = [
      alarms, codetectors, contacts, indicators, modes, motions, presences, relays,
      smokedetectors, switches, levels, temperatures, waterdetectors, location, accelerations,
      energymeters, musicplayers, lightSensor, powermeters, batteries, button,
      voltageMeasurement, lockDevice, humidities
    ]

    def states = devices.flatten().unique()
      .findAll { it?.hasProperty('capabilities') }
      .collect { deviceCapabilities it }
      .collect { deviceState it}
      .unique()
    logToHEC states
}

def deviceState(cap) {
    def splunk_server = "${splunk_host}:${splunk_port}"
    [
        event: cap,
        host: splunk_hec_host ?: splunk_server,
        sourcetype: "smartthings:state"
    ]
}

def deviceCapabilities(device) {
    [
        device: device.label,
        deviceId: device.id,
        capabilities: device.capabilities.collectEntries {
            it.attributes.collectEntries {
                [(it.name): device.currentValue(it.name)]
            }
        }
    ]
}

def logToHEC(events) {
    def local = use_local.toBoolean()
    def splunk_server = "${splunk_host}:${splunk_port}"

    // Write data locally using Hub Action (internal LAN IP ok)
    if (local == true) {
        def cmd = new physicalgraph.device.HubAction([
            method: 'POST',
            path: '/services/collector/event',
            headers: [
                'HOST': splunk_server, // used by the ST hub as endpoint
                'Authorization': "Splunk ${splunk_token}",
                'Content-Type': 'application/json',
            ],
            body: events.collect(JsonOutput.&toJson).join('\n')
        ], null, [callback: responseCallback])
        log.trace cmd
        sendHubCommand(cmd); // do it! (or fail silently, no exceptions thrown)
    }
    // Write data via ST cloud (must ensure Splunk HEC IP and port are publicly accessible)
    else {
        def ssl = use_ssl.toBoolean()
        def http_protocol = ssl ? 'https' : 'http'
        log.debug "Use Remote"

        def params = [
            uri: "${http_protocol}://${splunk_server}/services/collector/event",
            headers: [
                'Authorization': splunk_token,
                'Content-Type': 'application/json',
            ],
            body: events.collect(JsonOutput.&toJson).join('\n')
        ]
        log.debug params
        try {
            httpPostJson(params) // do it!
        } catch ( groovyx.net.http.HttpResponseException ex ) {
            log.debug "Unexpected response error: ${ex.statusCode}"
        }
    }
}

// Build JSON object and write it to Splunk HEC
// event specification: https://docs.smartthings.com/en/latest/ref-docs/event-ref.html
def genericHandler(evt) {
    log.debug "${evt.id}"
    def event = [
        date:                "${evt.date}",
        name:                "${evt.name}",
        displayName:         "${evt.displayName}",
        device:              "${evt.device}",
        deviceId:            "${evt.deviceId}",
        value:               "${evt.value}",
        isStateChange:       "${evt.isStateChange()}",
        id:                  "${evt.id}",
        description:         "${evt.description}",
        descriptionText:     "${evt.descriptionText}",
        installedSmartAppId: "${evt.installedSmartAppId}",
        isoDate:             "${evt.isoDate}",
        isDigital:           "${evt.isDigital()}",
        isPhysical:          "${evt.isPhysical()}",
        location:            "${evt.location}",
        locationId:          "${evt.locationId}",
        unit:                "${evt.unit}",
        stSource:            "${evt.source}",
    ]
    def splunk_server = "${splunk_host}:${splunk_port}"
    def body = [
        event: event,
        host: splunk_hec_host ?: splunk_server,
        sourcetype: "smartthings:events"
    ]
    logToHEC([body])
}

def responseCallback(output) {
  log.trace "Response: ${output}"
}

// Today all of the subscriptions use the generic handler, but could be customized if needed
def alarmHandler(evt) {
    genericHandler(evt)
}

def coHandler(evt) {
    genericHandler(evt)
}

def indicatorHandler(evt) {
    genericHandler(evt)
}

def presenceHandler(evt) {
    genericHandler(evt)
}

def switchHandler(evt) {
    genericHandler(evt)
}

def smokeHandler(evt) {
    genericHandler(evt)
}

def levelHandler(evt) {
    genericHandler(evt)
}

def contactHandler(evt) {
    genericHandler(evt)
}

def temperatureHandler(evt) {
    genericHandler(evt)
}

def motionHandler(evt) {
    genericHandler(evt)
}

def modeHandler(evt) {
    genericHandler(evt)
}

def relayHandler(evt) {
    genericHandler(evt)
}

def waterHandler(evt) {
    genericHandler(evt)
}

def locationHandler(evt) {
    genericHandler(evt)
}

def accelerationHandler(evt) {
    genericHandler(evt)
}

def energyHandler(evt) {
    genericHandler(evt)
}

def musicHandler(evt) {
    genericHandler(evt)
}
def illuminanceHandler(evt) {
    genericHandler(evt)
}

def powerHandler(evt) {
    genericHandler(evt)
}

def humidityHandler(evt) {
    genericHandler(evt)
}

def batteryHandler(evt) {
    genericHandler(evt)
}

def buttonHandler(evt) {
    genericHandler(evt)
}

def voltageHandler(evt) {
    genericHandler(evt)
}

def lockHandler(evt) {
    genericHandler(evt)
}
