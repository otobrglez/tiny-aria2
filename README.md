# tiny-aria2

[tiny-aria2][tiny-aria2] is a small and simple user interface / UI for [aria2](https://aria2.github.io/).

It connects to `aria2` via its [JSON-RPC](https://aria2.github.io/manual/en/html/aria2c.htm) interface.

## Demo

https://github.com/otobrglez/tiny-aria2/assets/225946/0f65b3b0-4034-4dbb-8301-4459c3ab3512

## Environment variables

- ```PORT``` - Port on which the service listens.
- ```ARIA2_URI``` - URI / URL where aria2 service / daemon is listening to
- ```ARIA2_USERNAME``` - `aria2` username
- ```ARIA2_PASSWORD``` - `aria2` password

## Development

To build and run a Docker Image:

```bash
sbt docker:publishLocal

docker run -ti --rm \
  -e PORT=4447 \
  -p 4448:4447 \
  -e ARIA2_URI=http://aria2host \
  -e ARIA2_USERNAME=pirate123 \
  -e ARIA2_PASSWORD=pirate123 \
  docker.io/pinkstack/tiny-aria2:latest
```

To build a fat-jar one shall use:

```bash
sbt assembly

java -jar target/*/tiny-aria2.jar
```

## aria2 Docker Image

Run `aria2` locally or build an image with something like this:

```Dockerfile
FROM ubuntu

RUN apt-get update -yy && \
    apt-get install aria2 -yy && \
    apt-get auto-remove -y && \
    apt-get auto-clean

EXPOSE 6800

CMD ["aria2c", \
    "--enable-rpc", \
    "--rpc-listen-all", \
    "--rpc-user", "pirate", \
    "--rpc-passwd", "pirate", \
    "--rpc-allow-origin-all", \
    "--max-concurrent-downloads", "5", \
    "--log-level", "info", \
    "--console-log-level", "info", \
    "--save-session-interval", "10", \
    "--dir", "/data", \
    "--save-session", "/data/aria2c-session.txt", \
    "--seed-ratio", "0.1" \
]
```

## Author

- [Oto Brglez](https://github.com/otobrglez)

[tiny-aria2]: https://github.com/otobrglez/tiny-aria2
