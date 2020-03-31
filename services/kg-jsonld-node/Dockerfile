FROM node:12-alpine
WORKDIR /app
COPY package*.json /app/
RUN npm install
COPY . /app
ENV NODE_OPTS=""
CMD exec node $NODE_OPTS bin/www
EXPOSE 3000