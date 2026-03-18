#!/bin/bash
cd /app/backend
npx prisma migrate deploy
node dist/server.js
