import unittest

from fastapi.testclient import TestClient

from backend.server import app


class SearchApiCompatibilityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    def test_search_route_supports_trailing_slash(self):
        response = self.client.get('/search/', params={'query': 'hello', 'limit': 2})
        self.assertEqual(response.status_code, 200, response.text)
        payload = response.json()
        self.assertIn('items', payload)
        self.assertIsInstance(payload['items'], list)

    def test_cors_preflight_allows_common_headers(self):
        response = self.client.options(
            '/search',
            headers={
                'Origin': 'https://example.com',
                'Access-Control-Request-Method': 'GET',
                'Access-Control-Request-Headers': 'authorization,user-agent',
            },
        )
        self.assertEqual(response.status_code, 200, response.text)
        self.assertEqual(response.headers.get('access-control-allow-origin'), 'https://example.com')
        self.assertIn('GET', response.headers.get('access-control-allow-methods', ''))
        allow_headers = response.headers.get('access-control-allow-headers', '').lower()
        self.assertIn('authorization', allow_headers)
        self.assertIn('user-agent', allow_headers)


if __name__ == '__main__':
    unittest.main()
