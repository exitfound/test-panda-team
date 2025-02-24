import threading
import time
import requests
from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO
from src.constants import HOST, PORT

class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Hello, World!')

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        body = self.rfile.read(content_length)
        self.send_response(200)
        self.end_headers()
        response = BytesIO()
        response.write(b'This is POST request. ')
        response.write(b'Received: ')
        response.write(body)
        self.wfile.write(response.getvalue())

def start_webserver():
    print(f"Web-server is starting on {HOST}:{PORT}")
    httpd = HTTPServer((HOST, PORT), SimpleHTTPRequestHandler)
    httpd.serve_forever()

def test_webserver():
    time.sleep(1)

    try:
        response = requests.get(f"http://{HOST}:{PORT}")
        assert response.status_code == 200, "GET request failed"
        assert response.text == "Hello, World!", "Unexpected response body"
        print("GET request test passed")

        response = requests.post(f"http://{HOST}:{PORT}", data="test data")
        assert response.status_code == 200, "POST request failed"
        assert "Received: test data" in response.text, "Unexpected response body"
        print("POST request test passed")

    except Exception as e:
        print(f"Test failed: {e}")

if __name__ == "__main__":
    server_thread = threading.Thread(target=start_webserver, daemon=True)
    server_thread.start()

    time.sleep(1)

    test_webserver()

    while True:
        time.sleep(1)
