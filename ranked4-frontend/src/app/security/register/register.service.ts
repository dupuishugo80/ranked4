import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { RegisterRequest } from "../login/login.model";
import { inject, Injectable } from "@angular/core";

@Injectable({
  providedIn: 'root'
})
export class RegisterService {
    private readonly API_URL = 'http://localhost:8080/api/auth';

    private http = inject(HttpClient);

    register(payload: RegisterRequest): Observable<any> {
        return this.http.post(`${this.API_URL}/register`, payload);
    }
}