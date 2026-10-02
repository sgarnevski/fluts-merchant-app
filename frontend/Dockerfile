# Self-contained: builds only from frontend/ (the generated API types are committed).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run lint && npm test && npm run build

FROM nginx:stable-alpine
# The template is rendered with envsubst at startup (FLUT_APP_URL = where flut-app runs).
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
ENV FLUT_APP_URL=http://flut-app:8081
EXPOSE 80
