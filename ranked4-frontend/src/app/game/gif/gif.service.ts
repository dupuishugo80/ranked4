import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../core/config/api.config';
import { Observable, shareReplay } from 'rxjs';
import { Gif } from './gif.model';

@Injectable({ providedIn: 'root' })
export class GifService {
    private gifs$?: Observable<Gif[]>;

    constructor(private http: HttpClient) {}

    getGifs(): Observable<Gif[]> {
        if (!this.gifs$) {
        this.gifs$ = this.http
            .get<Gif[]>(`${API_BASE_URL}/gifs`)
            .pipe(shareReplay(1));
        }
        return this.gifs$;
    }
}