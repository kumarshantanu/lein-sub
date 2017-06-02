# Changes and TODO

## UNRELEASED / 0.4.0

lein-sub will now automatically determine the optimum execution order.

Sub-modules may be specified as `--submodules` or `-s`.
Previously, only `-s` was supported.

Builds are now resumable using the `--resume` (or `-r`).

## 2013-Sep-22 / 0.3.0

* Add `-s <subprojects>` option support (Shantanu Kumar)

## 2012-Nov-03 / 0.2.4

* Add support for resolving aliases in project.clj (Creighton Kirkendall)

## 2012-Sep-30 / 0.2.3

* Init sub-projects, so their plugins are applied (Hugo Duncan)

## 2012-Sep-06 / 0.2.2

* Remove call to main/exit and rely on exceptions (Hugo Duncan)

## 2012-Aug-29 / 0.2.1

* Do not return integer for Leiningen 2 support (Shantanu Kumar)

## 2012-Feb-22 / 0.2.0

* Leiningen 2 support (Phil Hagelberg)
