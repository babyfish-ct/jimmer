import type {CompositeBookInput_TargetOf_authors, CompositeBookInput_TargetOf_store} from './';

export interface CompositeBookInput {
    readonly id?: number | undefined;
    readonly name: string;
    readonly edition: number;
    readonly price: number;
    readonly store?: CompositeBookInput_TargetOf_store | undefined;
    readonly authors: ReadonlyArray<CompositeBookInput_TargetOf_authors>;
}
