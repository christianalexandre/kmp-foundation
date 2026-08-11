.DEFAULT_GOAL := help
SIMULATOR ?= iPhone 17 Pro
# Override with a UDID where the name lookup fails: SIM_DEST='id=<sim-udid>'
SIM_DEST ?= platform=iOS Simulator,name=$(SIMULATOR)
BUNDLE_ID := com.kmpfoundation.iosApp
DERIVED_DATA := build/ios-derived-data

.PHONY: bootstrap gen open-ios check test lint format coverage android android-run ios ios-run clean help

bootstrap: ## Install tools, wire git hooks, generate the Xcode project
	brew bundle
	lefthook install
	$(MAKE) gen

gen: ## Regenerate the Xcode project (iosApp)
	cd iosApp && xcodegen generate

open-ios: gen ## Generate and open the Xcode project
	open iosApp/iosApp.xcodeproj

check: ## Full local gate: tests (JVM + iOS) + coverage + detekt + swiftlint + lint
	./gradlew check
	swiftlint lint --strict --quiet

test: ## Run the multiplatform tests only
	./gradlew allTests

lint: ## Static analysis: detekt (inclui build-logic) + swiftlint
	./gradlew detekt detektBuildLogic
	swiftlint lint --strict --quiet

format: ## Auto-fix what detekt/ktlint and swiftlint can fix on their own
	./gradlew detekt --auto-correct
	swiftlint --fix --quiet

coverage: ## Verify the 80% gate and open the HTML coverage report
	./gradlew koverVerify koverHtmlReport
	open composeApp/build/reports/kover/html/index.html

android: ## Build the Android debug APK
	./gradlew :androidApp:assembleDebug

android-run: ## Install and launch on the connected Android device/emulator
	./gradlew :androidApp:installDebug
	adb shell monkey -p com.kmpfoundation -c android.intent.category.LAUNCHER 1

ios: gen ## Build the iOS app for the simulator
	cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp \
		-configuration Debug -destination '$(SIM_DEST)' \
		-derivedDataPath ../$(DERIVED_DATA) -quiet build

ios-run: ios ## Build, install and launch the app on the simulator
	@# O UDID é resolvido pelo nome porque `booted` instala no simulador errado
	@# quando há mais de um ligado — e o app roda desatualizado, sem aviso.
	@set -e; \
	udid=$$(xcrun simctl list devices available | grep -m1 '$(SIMULATOR) (' \
		| grep -oE '[0-9A-Fa-f-]{36}'); \
	test -n "$$udid" || { echo "simulador '$(SIMULATOR)' não encontrado"; exit 1; }; \
	xcrun simctl boot $$udid 2>/dev/null || true; \
	open -a Simulator; \
	xcrun simctl install $$udid \
		$(DERIVED_DATA)/Build/Products/Debug-iphonesimulator/iosApp.app; \
	xcrun simctl launch $$udid $(BUNDLE_ID)

clean: ## Remove build artifacts and the generated Xcode project
	./gradlew clean
	rm -rf $(DERIVED_DATA) iosApp/iosApp.xcodeproj

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'
