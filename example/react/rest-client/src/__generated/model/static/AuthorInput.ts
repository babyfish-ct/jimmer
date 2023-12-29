import type {Gender} from '../enums/';

export interface AuthorInput {
    readonly id?: number | undefined;
    readonly firstName: string;
    readonly lastName: string;
    readonly gender: Gender;
}
