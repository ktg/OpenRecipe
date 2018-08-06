# create a file named Dockerfile
FROM node:8-slim
RUN mkdir /app
WORKDIR /app
COPY package.json /app
RUN npm install
CMD ["npm", "start"]