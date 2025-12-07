export interface Gif {
  id: number;
  code: string;
  assetPath: string;
  active: boolean;
}

export interface CreateGifRequest {
  code: string;
  assetPath: string;
  active?: boolean;
}

export interface UpdateGifRequest {
  code?: string;
  assetPath?: string;
  active?: boolean;
}
