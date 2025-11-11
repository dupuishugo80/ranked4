export interface Gif {
    id: number;
    code: string;
    assetPath: string;
    description?: string;
}

export interface GifReactionEvent {
    gameId: string;
    playerId: string;
    gifCode: string;
    assetPath: string;
    timestamp: number;
}