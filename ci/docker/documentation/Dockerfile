FROM ruby:2.7.1-alpine3.12
# mdl: https://github.com/markdownlint/markdownlint
RUN gem install mdl -v 0.11.0

RUN apk update \
    && apk add --update \
    git \
    curl \
    openssh-client \
    sudo \
    ca-certificates \
    # libc libraries are used to overcome https://github.com/gohugoio/hugo/issues/3382 😕
    libc6-compat libstdc++

# https://github.com/gohugoio/hugo/releases
ARG HUGO_VERSION=0.74.3

RUN curl -L https://github.com/gohugoio/hugo/releases/download/v${HUGO_VERSION}/hugo_extended_${HUGO_VERSION}_Linux-64bit.tar.gz --output /tmp/hugo.tar.gz \
    && tar xzf /tmp/hugo.tar.gz -C /tmp/ \
    && mv /tmp/hugo /usr/local/bin/ \
    && rm -rf /tmp/hugo \
    && rm /var/cache/apk/* \
    && hugo version

COPY entrypoint.sh /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
