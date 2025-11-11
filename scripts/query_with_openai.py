from openai import OpenAI

client = OpenAI()

url = 'https://mediocre-wiggliest-opal.ngrok-free.dev'
access_token = ''

response = client.responses.create(
    model="gpt-5",
    tools=[
            {
                "type": "mcp",
                "server_label": "db-proxy",
                "server_url": f"{url}/mcp/",
                "require_approval": "never"
            },
    ],
    # put structured arguments at top-level so the server receives them
    input="Give me 1 user from the database.",
)

print(response.output_text)