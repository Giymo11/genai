# React App

A simple React app that renders "React says: Hello!" and runs on port 5050.

## Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Start the Development Server

```bash
npm start
```

The app will be available at `http://localhost:5050`.

## Testing the App

Open your browser and navigate to:
```
http://localhost:5050
```

You should see the message "React says: Hello!".

## Running with Docker

### Build the Docker Image

```bash
./build.sh
```

This will create a Docker image named `genai-react-app:latest`.

### Run the Container

```bash
docker run -p 5050:5050 genai-react-app:latest
```

The app will be accessible at `http://localhost:5050`.

### Stop the Container

Find the container ID:
```bash
docker ps
```

Stop the container:
```bash
docker stop <container_id>
```

