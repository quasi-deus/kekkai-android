.PHONY: build lint test install

build:
	./gradlew :app:assembleDebug

lint:
	./gradlew :app:lint

test:
	./gradlew :app:compileDebugKotlin

install:
	adb install -r app/build/outputs/apk/debug/app-debug.apk
