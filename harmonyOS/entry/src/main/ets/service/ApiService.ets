import { http } from '@kit.NetworkKit';

const KEY_BASE_URL: string = 'baseUrl';
const KEY_COOKIE: string = 'sessionCookie';
const DEFAULT_BASE_URL: string = 'http://10.0.2.2:8081/practical-training';

export class ApiService {
  static setBaseUrl(url: string): void {
    AppStorage.setOrCreate(KEY_BASE_URL, url);
  }

  static getBaseUrl(): string {
    const storageVal: Object | undefined = AppStorage.get(KEY_BASE_URL);
    let url: string = DEFAULT_BASE_URL;
    if (storageVal !== undefined) {
      url = String(storageVal);
    }
    let value = url.trim();
    while (value.length > 0 && value.charAt(value.length - 1) === '/') {
      value = value.substring(0, value.length - 1);
    }
    return value;
  }

  static getCookie(): string {
    const storageVal: Object | undefined = AppStorage.get(KEY_COOKIE);
    if (storageVal !== undefined) {
      return String(storageVal);
    }
    return '';
  }

  static buildHeaders(): Record<string, string> {
    let headers: Record<string, string> = {
      'Content-Type': 'application/json'
    };
    const cookie = ApiService.getCookie();
    if (cookie.length > 0) {
      headers = {
        'Content-Type': 'application/json',
        'Cookie': cookie
      };
    }
    return headers;
  }

  static saveCookie(rawHeader: Object): void {
    if (!rawHeader) {
      return;
    }
    const keys: string[] = Object.keys(rawHeader);
    const values: Object[] = Object.values(rawHeader);
    for (let i = 0; i < keys.length; i++) {
      if (keys[i].toLowerCase() === 'set-cookie') {
        const cookieVal: string = String(values[i]);
        AppStorage.setOrCreate(KEY_COOKIE, cookieVal.split(';')[0]);
        return;
      }
    }
  }

  static async get(path: string): Promise<string> {
    const httpRequest = http.createHttp();
    try {
      const reqOptions: http.HttpRequestOptions = {
        method: http.RequestMethod.GET,
        header: ApiService.buildHeaders(),
        connectTimeout: 10000,
        readTimeout: 10000
      };
      const response = await httpRequest.request(ApiService.getBaseUrl() + path, reqOptions);
      ApiService.saveCookie(response.header);
      return String(response.result);
    } finally {
      httpRequest.destroy();
    }
  }

  static async post(path: string, body: Object): Promise<string> {
    const httpRequest = http.createHttp();
    try {
      const reqOptions: http.HttpRequestOptions = {
        method: http.RequestMethod.POST,
        header: ApiService.buildHeaders(),
        extraData: JSON.stringify(body),
        connectTimeout: 10000,
        readTimeout: 10000
      };
      const response = await httpRequest.request(ApiService.getBaseUrl() + path, reqOptions);
      ApiService.saveCookie(response.header);
      return String(response.result);
    } finally {
      httpRequest.destroy();
    }
  }
}
