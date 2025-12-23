from flask import Flask, jsonify
import os

app = Flask(__name__)

@app.route('/hello', methods=['GET'])
def hello_world():
    return jsonify({
        'message': 'Hello, World!',
        'status': 'success'
    })

if __name__ == '__main__':
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", 5005))
    app.run(host=host, port=port, debug=True)

