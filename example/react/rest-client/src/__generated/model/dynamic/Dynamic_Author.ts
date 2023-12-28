import type {Gender} from '../enums/';
import type {Dynamic_Book} from './';

export interface Dynamic_Author {
    readonly id?: number;
    readonly firstName?: string;
    readonly lastName?: string;
    readonly gender?: Gender;
    readonly books?: ReadonlyArray<Dynamic_Book>;
    readonly fullName?: string;
}
