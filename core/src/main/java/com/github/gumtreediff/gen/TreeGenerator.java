/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.gen;

import com.github.gumtreediff.tree.TreeContext;

import java.io.*;

public abstract class TreeGenerator {

    protected abstract TreeContext generate(Reader r, boolean preProcess) throws IOException;

    public TreeContext generateFromReader(Reader r, boolean preProcess) throws IOException {
        TreeContext ctx = generate(r, preProcess);
        ctx.validate();
        return ctx;
    }

    public TreeContext generateFromFile(String path, boolean preProcess) throws IOException {
        return generateFromReader(new FileReader(path), preProcess);
    }

    public TreeContext generateFromFile(File file, boolean preProcess) throws IOException {
        return generateFromReader(new FileReader(file), preProcess);
    }

    public TreeContext generateFromStream(InputStream stream, boolean preProcess) throws IOException {
        return generateFromReader(new InputStreamReader(stream), preProcess);
    }

    public TreeContext generateFromString(String content, boolean preProcess) throws IOException {
        return generateFromReader(new StringReader(content), preProcess);
    }
}
