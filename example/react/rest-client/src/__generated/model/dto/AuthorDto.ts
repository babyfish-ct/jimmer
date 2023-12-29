import type {Gender} from '../enums/';

export type AuthorDto = {
    /**
     */
    'AuthorService/COMPLEX_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly firstName: string;
        readonly lastName: string;
        readonly gender: Gender;
        readonly books: ReadonlyArray<{
            readonly id: number;
            readonly createdTime: string;
            readonly modifiedTime: string;
            readonly name: string;
            readonly edition: number;
            readonly price: number;
            readonly store?: {
                readonly id: number;
                readonly createdTime: string;
                readonly modifiedTime: string;
                readonly name: string;
                readonly website?: string | undefined;
                readonly avgPrice: number;
            } | null | undefined;
        }>;
    }
    /**
     */
    'AuthorService/DEFAULT_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly firstName: string;
        readonly lastName: string;
        readonly gender: Gender;
    }
    /**
     */
    'AuthorService/SIMPLE_FETCHER': {
        readonly id: number;
        readonly firstName: string;
        readonly lastName: string;
    }
}
