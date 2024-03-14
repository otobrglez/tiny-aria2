# tiny-aria2

[tiny-aria2][tiny-aria2] is a small and simple user interface / UI for [aria2](https://aria2.github.io/).

It connects to `aria2` via its [JSON-RPC](https://aria2.github.io/manual/en/html/aria2c.htm) interface.

## Demo

https://github.com/otobrglez/tiny-aria2/assets/225946/0f65b3b0-4034-4dbb-8301-4459c3ab3512


## Development

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

## Author

- [Oto Brglez](https://github.com/otobrglez)

[tiny-aria2]: https://github.com/otobrglez/tiny-aria2
