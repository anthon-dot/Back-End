// 1. Load the environment variables from your .env file
require('dotenv').config();
const { Client } = require('pg');

// 2. Initialize the Postgres client using your string
const client = new Client({
  connectionString: process.env.DATABASE_URL,
});

async function run() {
  try {
    // 3. Connect to Supabase
    await client.connect();
    console.log("🚀 Successfully connected to Supabase!");

    // 4. Run a test query to get the current time from the DB
    const res = await client.query('SELECT NOW();');
    console.log("🕒 Supabase Server Time:", res.rows[0].now);

  } catch (err) {
    console.error("❌ Connection failed:", err.message);
  } finally {
    // 5. Always close the connection when done
    await client.end();
  }
}

run();