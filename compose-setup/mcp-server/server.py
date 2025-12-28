# Added RAG functionality to expose rag/query.py via a REST endpoint.
# This way, the frontend/LLM can perform semantic search on the ingested recipes when they call the MCP server.
from rag.query import search_recipes
from flask import Flask, jsonify, request
import os

app = Flask(__name__)

@app.route('/hello', methods=['GET'])
def hello_world():
    return jsonify({
        'message': 'Hello, World!',
        'status': 'success'
    })
    
# Added for RAG semantic search
@app.route("/rag/search", methods=["GET"])
def rag_search():
    query = request.args.get("q", "")
    k = int(request.args.get("k", 5))
    if not query:
        return jsonify({"error": "Query parameter 'q' is required"}), 400

    results = search_recipes(query, k)
    return jsonify(results)

if __name__ == '__main__':
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", 5005))
    app.run(host=host, port=port, debug=True)