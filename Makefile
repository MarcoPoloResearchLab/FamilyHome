.PHONY: ci test test-service build-service build-android test-android test-android-contract test-android-upgrade release publish deploy

ci: test-service build-service test-android-contract

test: test-service test-android-contract

test-service:
	cd service && go test ./...

build-service:
	cd service && go build -o /dev/null .

build-android:
	cd android && ./build.sh

test-android: test-android-contract test-android-upgrade

test-android-contract:
	cd android && ./tests/apk-contract.sh

test-android-upgrade:
	cd android && ./tests/upgrade-persistence.sh

release publish deploy:
	@application_root="$$(git rev-parse --show-toplevel)"; \
	gateway_root="$$(dirname "$${application_root}")/mprlab-gateway"; \
	if [ ! -d "$${gateway_root}" ]; then \
		printf "required sibling gateway is missing: %s; clone mprlab-gateway at exactly %s\n" \
			"$${gateway_root}" "$${gateway_root}" >&2; \
		exit 2; \
	fi; \
	$(MAKE) --no-print-directory -C "$${gateway_root}" "app-$@" \
		MPRLAB_APP_ROOT="$${application_root}"
