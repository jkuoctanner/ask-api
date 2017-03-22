FROM quay.octanner.io/base/oct-scala:2.11.7-sbt-0.13.12-play-2.5.9

COPY . /app/

RUN sbt compile stage

ENTRYPOINT ["./start.sh"]
