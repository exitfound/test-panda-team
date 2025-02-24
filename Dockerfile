FROM python:3.11-slim-bookworm AS base

WORKDIR /app

COPY requirements.txt .

RUN --mount=type=cache,target=/root/.cache \
    pip install --user --no-cache-dir -r requirements.txt


FROM gcr.io/distroless/python3-debian12:latest AS final

LABEL service="http-server" \
      language="python" \
      version="3.11"

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV USER=nonroot

USER ${USER}

WORKDIR /app

COPY --from=base --chown=${USER}:${USER} /root/.local /home/${USER}/.local
COPY --chown=${USER}:${USER} . .

ENTRYPOINT [ "/usr/bin/python3.11", "webserver.py" ]
