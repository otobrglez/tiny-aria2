# tiny-aria2

[tiny-aria2][tiny-aria2] is a small user interface / UI for Aria2.
It connects to Aria2 via its [jsonrpc](https://aria2.github.io/manual/en/html/aria2c.htm) interface.

[tiny-aria2]: https://github.com/otobrglez/tiny-aria2

## Development

```bash
sbt docker:publishLocal

docker run -ti --rm \
  -e PORT=4447 \
  -p 4448:4447 \
  -e ARIA2_URI=http://aria2host \
  -e ARIA2_USERNAME=pirate123 \
  -e ARIA2_PASSWORD=pirate123 \
  docker.io/pinkstack/tiny-aria2:0.0.2
```

## Author

- [Oto Brglez](https://github.com/otobrglez)
