import { Api } from "../__generated";

const BASE_URL = "http://localhost:8080";

export const api = new Api(async({uri, method, body}) => {
    const response = await fetch(`${BASE_URL}${uri}`, {
        method,
        body: body !== undefined ? JSON.stringify(body) : undefined,
        headers: {
            'content-type': 'application/json;charset=UTF-8'
        }
    });
    if (response.status !== 200) {
        throw response.json();
    }
    const text = await response.text();
    if (text.length === 0) {
        return null;
    }
    return JSON.parse(text);
});