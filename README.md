# CatLink Binding

This binding integrates [CatLink](https://www.catlinkus.com/) smart pet devices
into openHAB through the CatLink cloud: self-cleaning litter boxes
(Scooper, C1, Open-X / Young Pro, Scooper Pro Ultra), automatic feeders, PurePro
water fountains, and per-cat health statistics.

It is an **independent, unofficial** integration and is not affiliated with or
endorsed by CatLink. The cloud protocol was derived from the community
[Home Assistant integration](https://github.com/hasscc/catlink). "CatLink" is a
trademark of its respective owner.

## Installation

Download `org.openhab.binding.catlink-<version>.jar` from the
[latest release](https://github.com/Slityak/openhab-catlink-binding/releases/latest)
and drop it into your openHAB `addons/` folder.

To build from source, clone the [openHAB add-ons](https://github.com/openhab/openhab-addons)
repository, place this module under `bundles/org.openhab.binding.catlink`, add it
to `bundles/pom.xml`, and run
`mvn clean install -pl :org.openhab.binding.catlink -am`.

## Supported Things

| Thing       | Type   | Cloud `deviceType`  | Description                                                  |
|-------------|--------|---------------------|--------------------------------------------------------------|
| `account`   | Bridge | –                   | A CatLink cloud account; performs login and discovery.       |
| `scooper`   | Thing  | `SCOOPER` (+fallback) | Generic self-cleaning litter box (also the fallback type). |
| `litterbox` | Thing  | `LITTER_BOX_599`    | Scooper C1 litter box.                                        |
| `c08`       | Thing  | `C08`               | Open-X / Young Pro litter box (full feature set).            |
| `proultra`  | Thing  | `VISUAL_PRO_ULTRA`  | Scooper Pro Ultra (read-only, limited support).             |
| `feeder`    | Thing  | `FEEDER`            | Automatic feeder.                                            |
| `purepro`   | Thing  | `PUREPRO`           | PurePro water fountain.                                      |
| `cat`       | Thing  | `CAT`               | A registered cat's health profile and daily statistics.     |

## Discovery

Create an `account` bridge with your credentials. A scan then discovers every
device shared with that account, dispatched to the matching thing type, plus a
`cat` thing for each registered cat. The device id (and pet id) are filled in
automatically.

## Thing Configuration

### `account` (bridge)

Log in with **either** an email address **or** a phone number — whichever you use
in the CatLink app.

| Parameter         | Type    | Required | Default  | Description                                                       |
|-------------------|---------|----------|----------|-------------------------------------------------------------------|
| `email`           | text    | no\*     |          | Account email address (use this **or** `phone`).                  |
| `phone`           | text    | no\*     |          | Account phone number, without the international prefix.            |
| `phoneIac`        | text    | no       | `86`     | International dial code for phone login, e.g. `36`, `1`.           |
| `password`        | text    | yes      |          | Account password (entered in plain text; encrypted before send).  |
| `region`          | text    | yes      | `global` | Server region: `global`, `usa`, `singapore` or `china`.           |
| `language`        | text    | no       | `en_US`  | `language` header sent to the API.                                |
| `refreshInterval` | integer | no       | `60`     | Device poll interval in seconds (minimum 15).                     |

\* Provide either `email` or `phone` (together with `password`).

> **One session per account.** The CatLink cloud allows only a single active
> session per account, so logging in here may log your app out (and vice versa).
> Create a dedicated shared sub-account for openHAB.

### Device things (`scooper`, `litterbox`, `c08`, `proultra`, `feeder`, `purepro`)

| Parameter    | Type | Required | Description                                          |
|--------------|------|----------|------------------------------------------------------|
| `deviceId`   | text | yes      | CatLink device id (filled in by discovery).          |
| `deviceType` | text | no       | Cloud device-type string (filled in by discovery).   |

### `cat`

| Parameter | Type | Required | Description                                |
|-----------|------|----------|--------------------------------------------|
| `petId`   | text | yes      | CatLink pet id (filled in by discovery).   |

## Channels

`R` = read-only, `RW` = readable and writable, `W` = command-only.

### Litter boxes — `c08` (most complete)

| Channel                 | Item Type | RW | Description                                                                  |
|-------------------------|-----------|----|------------------------------------------------------------------------------|
| `state`                 | String    | R  | Work status: `idle` / `running` / `need_reset`.                              |
| `mode`                  | String    | RW | Cleaning mode: `auto` / `manual` / `scheduled`.                              |
| `action`                | String    | W  | `clean-start` / `clean-pause` / `clean-cancel` / `pave-start` / `pave-pause`. |
| `litter-weight`         | Number    | R  | Current litter weight (kg).                                                  |
| `litter-remaining-days` | Number    | R  | Estimated days of litter remaining.                                         |
| `deodorant-countdown`   | Number    | R  | Deodorant remaining (days).                                                 |
| `total-clean-time`      | Number    | R  | Total clean count (induction + manual).                                     |
| `manual-clean-time`     | Number    | R  | Manual clean count.                                                          |
| `online`                | Switch    | R  | Device online state.                                                         |
| `last-log`              | String    | R  | Most recent event from the device log.                                      |
| `error`                 | String    | R  | Current error / message.                                                     |
| `litter-type`           | String    | RW | `bentonite` / `mixed`.                                                       |
| `safe-time`             | String    | RW | Safe time in minutes: `1`/`3`/`5`/`7`/`10`/`15`/`30`.                        |
| `quiet-mode`            | Switch    | RW | Quiet mode.                                                                  |
| `auto-burial`           | Switch    | RW | Automatic burial.                                                            |
| `continuous-cleaning`   | Switch    | RW | Continuous cleaning.                                                         |
| `child-lock`            | Switch    | RW | Panel child lock.                                                            |
| `indicator-light`       | Switch    | RW | Indicator light.                                                             |
| `keypad-tone`           | Switch    | RW | Keypad tone.                                                                 |
| `auto-pet-weight`       | Switch    | RW | Automatic pet-weight update.                                                 |
| `kitten-mode`           | Switch    | RW | Kitten mode.                                                                 |
| `wifi-rssi`             | Number    | R  | Wi-Fi signal strength.                                                       |
| `wifi-ssid`             | String    | R  | Wi-Fi network name.                                                          |
| `stats-times`           | Number    | R  | Usage statistics: clean count.                                              |
| `stats-weight-avg`      | Number    | R  | Usage statistics: average weight.                                          |
| `stats-duration-avg`    | Number    | R  | Usage statistics: average duration.                                        |
| `notice-*` (9 channels) | Switch    | RW | Push-notification toggles: `cat-came`, `box-full`, `replace-garbage-bag`, `wash-scooper`, `replace-deodorant`, `litter-not-enough`, `sandbox-not-enough`, `anti-pinch`, `firmware-updated`. |

### Litter boxes — `litterbox` (C1)

Has `state`, `mode` (`auto`/`manual`/`time`), `action` (`start`/`pause`),
`litter-weight`, `litter-remaining-days`, `deodorant-countdown`,
`total-clean-time`, `manual-clean-time`, `key-lock`, `online`, `last-log`,
`alarm`, `error`, plus:

| Channel                | Item Type | RW | Description                                          |
|------------------------|-----------|----|------------------------------------------------------|
| `garbage-status`       | String    | R  | `normal` / `movement_started` / `moving`.            |
| `box-full-sensitivity` | String    | RW | Box-full sensitivity level `1`–`4`.                  |
| `replace-garbage-bag`  | Switch    | W  | `ON` starts a bag change, `OFF` resets it.           |
| `reset-litter`         | Switch    | W  | `ON` resets the litter consumable counter.           |
| `reset-deodorant`      | Switch    | W  | `ON` resets the deodorant consumable counter.        |

### Litter boxes — `scooper` (generic / fallback)

`state`, `mode` (`auto`/`manual`/`time`/`empty`), `action` (`start`/`pause`),
`litter-weight`, `litter-remaining-days`, `temperature`, `humidity`,
`total-clean-time`, `manual-clean-time`, `deodorant-countdown`, `key-lock`,
`online`, `last-log`, `alarm`, `error`.

### Litter boxes — `proultra` (read-only)

`state`, `litter-remaining-days`, `deodorant-countdown`, `total-clean-time`,
`online`.

### `feeder`

| Channel                  | Item Type   | RW | Description                                     |
|--------------------------|-------------|----|-------------------------------------------------|
| `state`                  | String      | R  | Food-out status.                                |
| `portions`               | Number      | RW | Portions to dispense (1–10).                    |
| `feed`                   | Switch      | W  | `ON` dispenses `portions` portions.             |
| `weight`                 | Number:Mass | R  | Food weight.                                     |
| `auto-fill`              | String      | R  | Auto-fill status.                               |
| `indicator-light-status` | String      | R  | Indicator-light status.                         |
| `breath-light-status`    | String      | R  | Breath-light status.                            |
| `power-supply-status`    | String      | R  | Power-supply status.                            |
| `key-lock`               | Switch      | R  | Key lock state.                                 |
| `online`                 | Switch      | R  | Device online state.                            |
| `last-log`               | String      | R  | Most recent feeder event.                       |
| `error`                  | String      | R  | Current error / message.                        |

### `purepro`

| Channel        | Item Type            | RW | Description                                |
|----------------|----------------------|----|--------------------------------------------|
| `state`        | String               | R  | Current run state.                         |
| `mode`         | String               | RW | Run mode: `flowing` / `eco` / `smart`.     |
| `water-level`  | Number:Dimensionless | R  | Water level (%).                           |
| `filter-life`  | Number:Dimensionless | R  | Filter life remaining (%).                 |
| `temperature`  | Number:Temperature   | R  | Water temperature.                         |
| `uv-active`    | Switch               | R  | UV sterilization active.                   |
| `heating`      | Switch               | R  | Water heating active.                      |
| `light-active` | Switch               | R  | Light active.                              |
| `hair-cleaning`| Switch               | R  | Hair-cleaning active.                      |
| `online`       | Switch               | R  | Device online state.                       |
| `last-log`     | String               | R  | Most recent fountain event.                |

### `cat`

| Channel             | Item Type   | RW | Description                                                       |
|---------------------|-------------|----|-------------------------------------------------------------------|
| `status`            | String      | R  | Health status description.                                        |
| `weight`            | Number:Mass | R  | Registered body weight.                                           |
| `age-years`         | Number      | R  | Age, years.                                                       |
| `age-months`        | Number      | R  | Age, months.                                                      |
| `gender`            | String      | R  | `male` / `female` / `neutered_male` / `neutered_female`.          |
| `breed`             | String      | R  | Breed.                                                            |
| `toilet-times`      | Number      | R  | Toilet visits today.                                             |
| `toilet-weight-avg` | Number      | R  | Average body weight measured at the litter box (kg).             |
| `pee-times`         | Number      | R  | Pee count today.                                                  |
| `poo-times`         | Number      | R  | Poo count today.                                                  |
| `drink-times`       | Number      | R  | Drink count today.                                               |
| `diet-times`        | Number      | R  | Feeding count today.                                            |
| `diet-intakes`      | Number      | R  | Food intake today.                                              |
| `sport-duration`    | Number      | R  | Active duration today.                                          |

## Full Example

`catlink.things`:

```java
Bridge catlink:account:home "CatLink Account" [ email="you@example.com", password="secret", region="global", refreshInterval=60 ] {
    Thing c08 box   "Litter Box" [ deviceId="5843873" ]
    Thing cat genyo "Genyo"      [ petId="256809" ]
}
```

`catlink.items`:

```java
String      Box_State       "Status [%s]"       { channel="catlink:c08:home:box:state" }
String      Box_Mode        "Mode [%s]"         { channel="catlink:c08:home:box:mode" }
String      Box_Action      "Action"            { channel="catlink:c08:home:box:action" }
Number      Box_Litter      "Litter [%.1f kg]"  { channel="catlink:c08:home:box:litter-weight" }
Switch      Box_ChildLock   "Child Lock"        { channel="catlink:c08:home:box:child-lock" }
Switch      Box_QuietMode   "Quiet Mode"        { channel="catlink:c08:home:box:quiet-mode" }

Number:Mass Cat_Weight      "Weight [%.1f kg]"  { channel="catlink:cat:home:genyo:weight" }
String      Cat_Status      "Status [%s]"       { channel="catlink:cat:home:genyo:status" }
Number      Cat_ToiletTimes "Toilet [%d]"       { channel="catlink:cat:home:genyo:toilet-times" }
```

To change a mode or trigger an action from a rule or the console, send the option
value, e.g. `sendCommand(Box_Mode, "auto")` or
`sendCommand(Box_Action, "clean-start")`.

## Notes and Limitations

- **One active session per account** — see the bridge note above; use a dedicated
  sub-account for openHAB.
- **Per-cat statistics** only populate once the litter box has learned to
  recognise the cat over several visits; until then the cloud reports
  "Data collection in progress" and the counters read `0`. `toilet-weight-avg`
  reflects the registered weight until visit data exists.
- The `c08` boolean setting encoding (`enable=true/false`) and the quiet-mode
  command (which mirrors the Home Assistant integration's use of the
  `autoBurial` endpoint) are best-effort and may need adjustment on some
  firmware revisions.
- Unrecognised litter-box-like `deviceType`s fall back to the generic `scooper`
  thing so they can still be added and monitored.
