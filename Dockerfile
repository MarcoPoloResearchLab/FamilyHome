FROM docker.io/library/golang:1.26.5-alpine@sha256:0178a641fbb4858c5f1b48e34bdaabe0350a330a1b1149aabd498d0699ff5fb2 AS build

WORKDIR /src
COPY service/go.mod service/go.sum ./
RUN go mod download
COPY service/*.go ./
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -ldflags="-s -w" -o /out/familyhome-service . && \
    mkdir -p /out/data/drawings

FROM gcr.io/distroless/static-debian12:nonroot@sha256:afa5c872c891853ca7fcf1f12c3edb23f7eeef36189728842dd51042ff57f7ab

WORKDIR /app
COPY --from=build --chown=65532:65532 /out/familyhome-service /app/familyhome-service
COPY --from=build --chown=65532:65532 /out/data /data

USER 65532:65532
EXPOSE 8765
ENTRYPOINT ["/app/familyhome-service"]
