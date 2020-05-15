/**
 * **************************  Switch Dashboard **************************
 *
 * MIT License - see full license in repository LICENSE file
 * Copyright (c) 2020 Mattias Fornander (@mfornander)
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Description: "Turn your LED status switches into mini-dashboards"
 * Hubitat parent app to be installed with "Switch Dashboard Condition" child app.
 *
 * See getDescription() function body below for more details.
 *
 * Versions:
 * 1.0.0: 2020-05-14 - Initial release.
 * 1.1.0: 2020-05-15 - Added Inovelli Configuration Value
 */

/// Expose parent app version to allow version mismatch checks between child and parent
def getVersion() {
    "1.1.0"
}

// Set app Metadata for the Hub
definition(
    name: "Switch Dashboard",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Turn your LED status switches into mini-dashboards",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/SwitchDashboard/SwitchDashboard-parent.groovy",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: false
)

/**
 * Internal helper function providing the text displayed when the
 * user opens the 'Instructions' section.  Broken out to a separate
 * function to bring this text towards the top of the source and to
 * keep the mainPage preferences section clean and readable.
 */
private getDescription() {
"""<p>This parent-child app pair allows easy linking of Hubitat sensor states
to LEDs of your HomeSeer HS-WD200 dimmers and Inovelli Gen2 switches or
dimmers.  This is why I got excited to buy those dimmers but it was never
easy to use them so I wrote a real app to solve this once and for all.

<p>You can link states such as contact sensors open/closed, motion sensors
active/inactive, locks locked/unlocked, and more, to LEDs of various colors on
your switch/dimmer.  Several sensors can share an LED such that the same LED
can show yellow if a door is unlocked and red if open.

<p><b>Conditions</b><br>
Each set of dimmers can have one or more "Conditions" that link sensor states
to a specific LED index and color.  Conditions also have explicit priorities
that determine which condition gets to set an LED if there is a conflict.
This allows the lock+door example above to always show door open as red, and
only show yellow for unlocked if the door is closed.

<p><b>LED Sharing</b><br>
One Dashboard app can control more than one Dimmer such that several switches
and dimmers can show the same status.  However you can also install many
Dashboard apps if you want two dimmers to show different states.

<p><b>LED Indexing</b><br>
HomeSeer HS-WD200+ dimmer supports seven individually controllable LEDs while
the Inovelli Gen2 switch/dimmer can only be controlled as one. You can have
both types of dimmers share the same dashboard but the Inovelli will only
display LED index 1.  A dashboard with an important notification can use index
1 such that both types can show that condition and use index 2 through 7 for
less urgent conditions that are only displayed on HomeSeers.  Also note that
as of May 12 2020, the Inovelli doesn't support LED saturation in notifications
so the color "White" cannot be set.  Bug their support to add full HSB (hue,
satuation, brightness) capabilities in startNotification. See post at:
https://community.inovelli.com/t/feedback-after-using-the-driver-api-on-gen2-devices/3240

<p><b>Device and Driver Requirements</b><br>
This app does not control the LEDs directly and needs specific device drivers
to do its work.  Specifically it looks for the stock WD200+ driver's
setStatusLED() function and the official Inovelli driver's startNotification()
function to identify compatible switch dashboards.  There are other drivers out
there and if you have them installed, your mileage may vary.  File a bug report
and either I or the driver developer can look into it.

<p><b>Inovelli Configuration Value</b><br>
The Innovelli dimmer has the ability to display effects such as Chase, Pulse,
Blink, and many more I'm sure will be added in the future.  Instead of trying to
keep up with their effects with a UI, there is an optional COnfiguration Value
at the bottom of LED Indicator.  If set, this optional value is used instead of
the Color selection on Inovelli switches.  This means that a Condition that wins
on LED #1 may thus show Red on HomeSeers and a chasing pink on Inovelli switches.
In a house with only Inovellis that won't matter but it may be confusing. Note
that the duration of the value is ignored and instead force to infint since this
app should turn off the LED, not a duration.  The confguration value can be
computed at: <a href=https://nathanfiscus.github.io/inovelli-notification-calc>
https://nathanfiscus.github.io/inovelli-notification-calc</a>

<p><b>Sensor Types and Virtual Switches</b><br>
The current version supports a variety of sensors but there are many missing.
Make a bugreport or feature request on GitHub and I'll try to add it.
However, there is this workarond: You can use a Virtual Switch in an
automation such as RuleMachine with any level of complexity and sensor inputs,
and link a Condition.

<p><b>Background and Use Case</b><br>
The use case and inspiration for this app is my house with nine major doors and
several ground level windows to the outside.  I wanted to know at glance if the
doors were closed and/or locked since the neighborhood has seen an increase
in burglaries and we has some punk tug at the alleyway door 6am last week.  The
first version was a RuleMachine instance but it was not pretty write or
maintain, but more importantly I wanted to learn more Hubitat and Groovy.

<p><b>Source Code Notes</b><br>
I've complained previously on the Hubitat forums about how hard it it to get
into app and driver development.  In these apps I've taken the time write
comments that I would have appreciated when reading and learning from other
apps.  I still wish that Hubitat would spend time write better API docs but
until then, I hope that this commented code will help someone else get up to
speed faster.

<p><b><i>Note that this is my first Hubitat App and first time using Groovy
so don't trust it with anything important.</i></b>"""
}

/// Defer to mainPage() function to declare the preference UI
preferences {
    page name: "mainPage", title: "Switch Dashboard", install: true, uninstall: true
}

/**
 * Called after app is initially installed.
 */
def installed() {
    logDebug "Installed with settings: ${settings}"
    doRefreshDashboard()
}

/**
 * Called after any of the configuration settings are changed.
 */
def updated() {
    logDebug "Updated with settings: ${settings}"
    doRefreshDashboard()
}

/**
 * Main configuration function declares the UI shown.
 *
 * A simple UI that just requests a name of the app and allows the user to
 * add one or more conditions that will control the LEDs of the dimmers.
 *
 * Each Dashboard app instance allows the creation of a dashboard that
 * can be displayed on one or more dimmers.  You can add additional
 * instance of this app if you want different dashboards on different
 * dimmerd.
 *
 * The use of child apps may seem overly complicated at first and I went
 * through quite a few UI iterations before ending up here.  I really
 * wanted a simple single-source app but the dynamic nature of having
 * one or more dimmers sharing the same dashboard, and one or more conditions
 * per dashboard, with seven LEDs per dimmer and overloaded conditions
 * per LED... caused me to end up here.
 *
 * Debug logging is set per parent app instance and caues all its child
 * apps, i.e. conditions to also enable debg logging.  Instructions are
 * hidden at the bottom in a closed section since most people will only
 * read it once and then it should be out of the way.
 */
def mainPage() {
    checkNewVersion()
    dynamicPage(name: "mainPage") {
        section() {
            paragraph '<i>"Turn your LED status switches into mini-dashboards"</i>'
            label title: "Name", required: false
            // TODO: Allow only selection of Inovelli/HomeSeer switches (https://community.hubitat.com/t/device-specific-inputs/36734/7)
            input "devices", "capability.switch", title: "Switches (only HomeSeer WD200+ or Inovelli Gen2)", required: true, multiple: true, submitOnChange: true
            app name: "anyOpenApp", appName: "Switch Dashboard Condition", namespace: "MFornander", title: "Add LED status condition", multiple: true
            input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
            paragraph state.versionMessage
        }
        section("Instructions", hideable: true, hidden: true) {
            paragraph "<b>Switch Dashboard v${getVersion()}</b>"
            // Strip newlines to allow the description text to flow naturally
            paragraph getDescription().replaceAll("[\r\n]+"," ")
        }
    }
}

/**
 * Trigger a refresh of the dashboard, called by child apps.
 *
 * Called by child apps when their state or settings have changed.  The use
 * of runIn() is a workaround since this function is called in a child's
 * installed() and uninstalled() function but the parent's list of apps
 * is not updated until *after* those methods return.  To allow immediate
 * updates of new or removed conditons, I ask the parent to refresh its
 * dashboard in zero seconds which still happens after the installed or
 * uninstalled return.  A little messy but I really wanted the user to
 * see their settings and conditions on the dashboard right away.
 */
def refreshDashboard() {
    runIn(0, doRefreshDashboard)
}

/**
 * Main dashboard logic that reads all conditions and sets LEDs.
 *
 * I lost some time during development with mismatched child and parent code
 * so each refresh does a version match of parent and child.
 * Each condition gets a chance to add its color and index slot and stores
 * its priority along with that data.  Conditions only replace colors if
 * there is no previous color at that slot or if their priority is higher
 * than the current priority stored at the slot.
 *
 * In the end, all conditions have had their say and the leds map now
 * contains the intended color for each LED slot.  We first turn on the LEDs
 * that should be on and then after that, turn off the ones that should be
 * off.  This is to prevent a condition where the WD200 would temporarily
 * enter state where all LEDs are off and flash the current dimmer level
 * before setting LEDs again.
 *
 * TODO: Optimize by only calling setStatusLED if different from last set.
 */
def doRefreshDashboard() {
    def children = getChildApps()
    def fail = children.find { it.getVersion() != getVersion() }
    if (fail) log.error "Version mismatch: parent v${getVersion()} != child v${fail.getVersion()}"

    logDebug "Refreshing ${children.size()} conditions..."
    def leds = [:]
    children*.addCondition(leds)

    devices.each { device ->
        (1..7).each { if (leds[it]) setStatusLED(device, it, leds[it]) }
        (1..7).each { if (!leds[it]) setStatusLED(device, it, [color: "Off"]) }
    }

    state.leds = leds
    logDebug "LEDs: ${state.leds}"
}

/**
 * Internal LED control function for both HomeSeer and Inovelli.
 *
 * Both the HomeSeer dimmer and Inovelli dimmer/switch support setting the LED(s)
 * using their cown configuration commands but they are quite different.
 * This function provides abstraction to treat them as they both support the
 * SetStatusLED command and simplifies dashboard updating.
 */
private setStatusLED(device, index, status) {
    if (device.hasCommand("setStatusLED")) {
        // HomeSeer HS-WD200+ dimmer (7 controllable LEDs)
        switch (status.color) {
            case "Red":     device.setStatusLED(index as String, "1"); break
            case "Yellow":  device.setStatusLED(index as String, "5"); break
            case "Green":   device.setStatusLED(index as String, "2"); break
            case "Cyan":    device.setStatusLED(index as String, "6"); break
            case "Blue":    device.setStatusLED(index as String, "3"); break
            case "Magenta": device.setStatusLED(index as String, "4"); break
            case "White":   device.setStatusLED(index as String, "7"); break
            case "Off":     device.setStatusLED(index as String, "0"); break
            default:        log.error "Illegal status: ${status}"; break
        }
    } else if (device.hasCommand("startNotification")) {
        // Inovelli Gen2 switch or dimmer with their recent driver (1 controllable LED)
        if (index == 1) {
            if (status.inovelli) {
                device.startNotification(status.inovelli | 0xFF0000) // Force duration to infinity
            } else {
                // See https://nathanfiscus.github.io/inovelli-notification-calc
                long baseValue = 0x01ffff00 // Solid=01, Bright=FF, Forever=FF, Hue=00
                long hueIncrement = 256/6
                switch (status.color) {
                    case "Red":     device.startNotification(baseValue | 0*hueIncrement); break
                    case "Yellow":  device.startNotification(baseValue | 1*hueIncrement); break
                    case "Green":   device.startNotification(baseValue | 2*hueIncrement); break
                    case "Cyan":    device.startNotification(baseValue | 3*hueIncrement); break
                    case "Blue":    device.startNotification(baseValue | 4*hueIncrement); break
                    case "Magenta": device.startNotification(baseValue | 5*hueIncrement); break
                    case "White":
                        device.startNotification(baseValue) // Red
                        log.error "${device.displayName}: Inovelli doesn't support white (ask their support for 'startNotification saturation')"
                        break
                    case "Off":     device.stopNotification(); break
                    default:        log.error "Illegal status: ${status}"; break
                }
            }
        }
    } else {
        log.error(
            "${device.displayName} is not a usable HomeSeer or Inovelli device" +
            " (ID:${device.id}, Name:'${device.name}' Type:'${device.typeName}')")
    }
}

/**
 * Internal SemVer comparator function, with fancy spaceships.
 *
 * Return 1 if the given version is newer than current, 0 if the same, or -1 if older,
 * according to http://semver.org
 */
private compareTo(version) {
    def newVersion = version.tokenize(".")*.toInteger()
    def runningVersion = getVersion().tokenize(".")*.toInteger()
    logDebug "Version new:${newVersion} running:${runningVersion}"
    if (newVersion.size != 3) throw new RuntimeException("Illegal version:${version}")

    if (newVersion[0] == runningVersion[0]) {
        if (newVersion[1] == runningVersion[1]) {
            newVersion[2] <=> runningVersion[2]
        } else {
            newVersion[1] <=> runningVersion[1]
        }
    } else {
        newVersion[0] <=> runningVersion[0]
    }
}

/**
 * Internal version check function.
 *
 * Download a version file and set state.versionMessage if there is a newer
 * version available.  This message is displayed in the settings UI.
 * TODO: Only do this once a day?
 */
private checkNewVersion() {
    def params = [
        uri: "https://raw.githubusercontent.com",
        path: "MFornander/Hubitat/master/apps/SwitchDashboard/version.json",
        contentType: "application/json",
        timeout: 3
    ]
    try {
        httpGet(params) { response ->
            logDebug "checkNewVersion response data: ${response.data}"
            switch (compareTo(response.data)) {
                case 1:
                    state.versionMessage = "(New app v${response.data} available, running is v${getVersion()})"
                    break
                case 0:
                    state.remove("versionMessage")
                    break
                default:
                    throw new RuntimeException("GitHub v${response.data} is older than running v${getVersion()}")
            }
        }
    } catch (e) {
        log.error "checkNewVersion error: ${e}"
        state.remove("versionMessage")
    }
}

/**
 * Internal helper debug logging function
 */
private logDebug(msg) {
    if (debugEnable) log.debug msg
}
