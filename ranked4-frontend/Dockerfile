FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* .npmrc* ./
RUN npm install

COPY . .
RUN npm run build -- --configuration production

FROM nginx:stable-alpine

COPY --from=build /app/dist/ranked4-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
