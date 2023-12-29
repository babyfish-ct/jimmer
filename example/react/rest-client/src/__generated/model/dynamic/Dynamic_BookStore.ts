import type {Dynamic_Book} from './';

export interface Dynamic_BookStore {
    readonly id?: number;
    readonly name?: string;
    readonly website?: string | undefined;
    readonly books?: ReadonlyArray<Dynamic_Book>;
    readonly avgPrice?: number;
    readonly newestBooks?: ReadonlyArray<Dynamic_Book>;
}
