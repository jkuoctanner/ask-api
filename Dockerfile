FROM quay.octanner.io/base/oct-scala:2.12.2-sbt-0.13.15-play-2.6.1

COPY . /app/

RUN sbt compile stage

ENTRYPOINT ["./start.sh"]
