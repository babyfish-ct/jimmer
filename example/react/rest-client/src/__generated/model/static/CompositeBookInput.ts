import type {CompositeBookInput_TargetOf_authors, CompositeBookInput_TargetOf_store} from './';

export interface CompositeBookInput {
    readonly id: number | null | undefined;
    readonly name: string;
    readonly edition: number;
    readonly price: number;
    readonly store: CompositeBookInput_TargetOf_store | null | undefined;
    readonly authors: ReadonlyArray<CompositeBookInput_TargetOf_authors>;
}
