.PHONY: test test-service build-android

test: test-service

test-service:
	cd service && go test ./...

build-android:
	cd android && ./build.sh
