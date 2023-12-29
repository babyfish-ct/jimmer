import type {Gender} from '../enums/';

export type BookDto = {
    /**
     */
    'BookService/COMPLEX_FETCHER': {
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
        readonly authors: ReadonlyArray<{
            readonly id: number;
            readonly createdTime: string;
            readonly modifiedTime: string;
            readonly firstName: string;
            readonly lastName: string;
            readonly gender: Gender;
        }>;
    }
    /**
     */
    'BookService/DEFAULT_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly name: string;
        readonly edition: number;
        readonly price: number;
        readonly store?: {
            readonly id: number;
            readonly name: string;
        } | null | undefined;
        readonly authors: ReadonlyArray<{
            readonly id: number;
            readonly firstName: string;
            readonly lastName: string;
        }>;
    }
    /**
     */
    'BookService/SIMPLE_FETCHER': {
        readonly id: number;
        readonly name: string;
        readonly edition: number;
    }
}
