# Simulator Guide

## Tracks

| Map ID | Name | Road Width | Length | Obstacles | Use |
|--------|------|-----------|--------|-----------|-----|
| 10 | Basic Round | 16m | 1,360m | No | Competition |
| 30 | Speed Racing | 16m | 1,860m | No | Practice |
| 31 | Speed Racing | 16m | 1,860m | Yes | Competition |
| 60 | SSAFY Track | 22m | 5,910m | No | Practice |
| 61 | SSAFY Track | 22m | 5,910m | Yes | Competition |
| 70 | SSAFY Track (low-spec) | 22m | 5,910m | No | Practice |
| 71 | SSAFY Track (low-spec) | 22m | 5,910m | Yes | Personal local PC |
| 160 | Germany Track | 14m | 4,574m | No | Practice |
| 161 | Germany Track | 14m | 4,574m | Yes | Competition |

## settings.json

Location: `C:\Users\{username}\Documents\AirSim\settings.json`

Pre-made presets are in `settings/`. Copy the desired file's contents to the path above, then restart the simulator.

| File | Map | Mode |
|------|-----|------|
| `settings_타임어택-베이직.json` | 10 | Single |
| `settings_타임어택-스피드.json` | 31 | Single |
| `settings_타임어택-싸피.json` | 61 | Single |
| `settings_타임어택-싸피_저사양.json` | 71 | Single (local) |
| `settings_타임어택-독일.json` | 161 | Single |
| `settings_배틀-싸피.json` | 61 | Multi |
| `settings_배틀-싸피_저사양.json` | 71 | Multi (local) |

## Running

- Single: `run.bat` → execute `MyCar.java`
- Multi (2-player): `runsv.bat` → launch each client in order
- Multi car positions: Car1 `Y=−4`, Car2 `Y=4`

## Shortcuts

| Key | Action |
|-----|--------|
| `F8` | Keyboard manual mode |
| `Backspace` | Reset simulator |
| `F1` | Help |
| `F` | FPV view |
| `B` | Follow view |
