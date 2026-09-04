.PHONY: build-match test-match-toolbar test-android-toolbar
.PHONY: ci test test-service build-service build-android test-android test-android-contract test-android-upgrade test-android-weather test-android-piano release publish deploy

ci: test-service build-service test-android-contract

test: test-service test-android-contract

test-service:
	cd service && go test ./...

build-service:
	cd service && go build -o /dev/null .

build-android:
	cd android && ./build.sh

test-android: test-android-contract test-android-upgrade test-android-guitar

test-android-contract:
	cd android && ./tests/apk-contract.sh

test-android-upgrade:
	cd android && ./tests/upgrade-persistence.sh

test-android-weather:
	cd android && bash ./tests/weather-widget.sh

test-android-piano:
	cd android && bash ./tests/piano-audio.sh

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

test-android-toolbar:
	cd android && bash ./tests/toolbar.sh

toolbar-test-deps:
	python3 -m venv android/build/toolbar-python
	android/build/toolbar-python/bin/pip --quiet install -r android/tests/toolbar-requirements.txt

test-match-toolbar: toolbar-test-deps
	PYTHONDONTWRITEBYTECODE=1 android/build/toolbar-python/bin/python -m pytest -q -s -o cache_dir=android/build/pytest-cache android/tests/match-toolbar.py

build-match:
	bash games/match-portal/build.sh

.PHONY: test-android-guitar
test-android-guitar:
	cd android && bash ./tests/guitar.sh

.PHONY: test-games-toolbar
test-games-toolbar: toolbar-test-deps
	ANDROID_SERIAL="$(ANDROID_SERIAL)" PYTHONDONTWRITEBYTECODE=1 android/build/toolbar-python/bin/python -m pytest -q -s -o cache_dir=android/build/pytest-cache android/tests/games-toolbar.py

.PHONY: build-blocks build-tiles toolbar-test-deps
build-blocks:
	bash games/build-toolbar-game.sh blocks

build-tiles:
	bash games/build-toolbar-game.sh tiles
